/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.sca;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Core SCA service that intercepts class loading, extracts JAR metadata asynchronously, and emits
 * one OTel log event per unique JAR to the {@code co.elastic.otel.sca} instrumentation scope.
 *
 * <p>Design constraints:
 *
 * <ul>
 *   <li>The {@link ClassFileTransformer#transform} method always returns {@code null} — bytecode is
 *       never modified.
 *   <li>Class-loading threads are never blocked; all I/O happens on a single daemon background
 *       thread.
 *   <li>Discovery uses {@link ProtectionDomain#getCodeSource()} rather than {@code
 *       ClassLoader.getResource()} to avoid holding the classloader monitor.
 * </ul>
 */
public final class JarCollectorService implements ClassFileTransformer {

  private static final java.util.logging.Logger log =
      java.util.logging.Logger.getLogger(JarCollectorService.class.getName());

  // ---- OTel attribute keys -----------------------------------------------

  private static final AttributeKey<String> ATTR_LIBRARY_NAME =
      AttributeKey.stringKey("library.name");
  private static final AttributeKey<String> ATTR_LIBRARY_VERSION =
      AttributeKey.stringKey("library.version");
  private static final AttributeKey<String> ATTR_LIBRARY_GROUP_ID =
      AttributeKey.stringKey("library.group_id");
  private static final AttributeKey<String> ATTR_LIBRARY_PURL =
      AttributeKey.stringKey("library.purl");
  private static final AttributeKey<String> ATTR_LIBRARY_JAR_PATH =
      AttributeKey.stringKey("library.jar_path");
  private static final AttributeKey<String> ATTR_LIBRARY_SHA256 =
      AttributeKey.stringKey("library.sha256");
  private static final AttributeKey<String> ATTR_LIBRARY_CLASSLOADER =
      AttributeKey.stringKey("library.classloader");
  private static final AttributeKey<String> ATTR_EVENT_NAME =
      AttributeKey.stringKey("event.name");
  private static final AttributeKey<String> ATTR_EVENT_DOMAIN =
      AttributeKey.stringKey("event.domain");

  // ---- Internal state ----------------------------------------------------

  /** Maximum number of pending JAR paths that can queue before drops begin. */
  private static final int QUEUE_CAPACITY = 500;

  private final OpenTelemetrySdk openTelemetry;
  private final Instrumentation instrumentation;
  private final SCAConfiguration config;

  /** Paths already enqueued or processed — prevents duplicate work. */
  private final Set<String> seenJarPaths = ConcurrentHashMap.newKeySet();

  /**
   * Bounded queue of JARs waiting for metadata extraction. Offer is non-blocking; full queue drops
   * the entry (class loading must never block).
   */
  private final LinkedBlockingQueue<PendingJar> pendingJars =
      new LinkedBlockingQueue<>(QUEUE_CAPACITY);

  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean stopped = new AtomicBoolean(false);

  /** Names/patterns to identify JARs that should never be reported. */
  private final String agentJarPath;

  private final String tmpDir;

  JarCollectorService(
      OpenTelemetrySdk openTelemetry, Instrumentation instrumentation, SCAConfiguration config) {
    this.openTelemetry = openTelemetry;
    this.instrumentation = instrumentation;
    this.config = config;
    this.agentJarPath = resolveAgentJarPath();
    this.tmpDir = normalise(System.getProperty("java.io.tmpdir", "/tmp"));
  }

  // ---- Lifecycle ---------------------------------------------------------

  void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }

    // Register transformer — returns null always, observes only
    instrumentation.addTransformer(this, /* canRetransform= */ false);

    // Back-fill classes already loaded before our transformer registered
    scanAlreadyLoadedClasses();

    // Single daemon thread handles all I/O off the class-loading path
    Thread worker = new Thread(this::processQueue, "elastic-sca-jar-collector");
    worker.setDaemon(true);
    worker.setPriority(Thread.MIN_PRIORITY);
    worker.start();

    // Drain remaining queue on JVM shutdown before the OTLP exporter shuts down
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  stopped.set(true);
                  worker.interrupt();
                },
                "elastic-sca-shutdown"));

    log.fine("SCA: JarCollectorService started");
  }

  // ---- ClassFileTransformer ----------------------------------------------

  /**
   * Called by the JVM on every class load. We extract the JAR path from the {@link
   * ProtectionDomain}, deduplicate, and offer to the background queue. We never transform the
   * bytecode.
   */
  @Override
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {
    // Skip bootstrap classloader (null) and already-stopped state
    if (loader == null || className == null || stopped.get()) {
      return null;
    }
    try {
      enqueueFromProtectionDomain(loader, protectionDomain);
    } catch (Exception ignored) {
      // Must never propagate out of transform()
    }
    return null;
  }

  // ---- Discovery helpers -------------------------------------------------

  private void enqueueFromProtectionDomain(ClassLoader loader, ProtectionDomain pd) {
    if (pd == null) {
      return;
    }
    CodeSource cs = pd.getCodeSource();
    if (cs == null) {
      return;
    }
    URL location = cs.getLocation();
    if (location == null) {
      return;
    }
    String jarPath = locationToJarPath(location);
    if (jarPath == null || !jarPath.endsWith(".jar")) {
      return;
    }
    if (shouldSkip(jarPath)) {
      return;
    }
    if (!seenJarPaths.add(jarPath)) {
      return; // already seen
    }

    String classloaderName = loader.getClass().getName();
    // Non-blocking offer: if the queue is full we drop this JAR rather than stall a class-loading
    // thread. Remove from seen-set so a future class load from the same JAR gets another chance.
    if (!pendingJars.offer(new PendingJar(jarPath, classloaderName))) {
      seenJarPaths.remove(jarPath);
      log.fine("SCA: queue full, dropping JAR (will retry on next class load): " + jarPath);
    }
  }

  /**
   * Converts a {@link CodeSource} location URL to an absolute filesystem path. Handles the common
   * {@code file:/path/to/foo.jar} form produced by most classloaders.
   */
  static String locationToJarPath(URL location) {
    try {
      if ("file".equals(location.getProtocol())) {
        // Use URI to correctly handle spaces (%20) and other encoded chars
        return new File(location.toURI()).getAbsolutePath();
      }
      // jar:file:/path/to/outer.jar!/  — nested JAR (Spring Boot, etc.)
      if ("jar".equals(location.getProtocol())) {
        String path = location.getPath(); // file:/path/to/outer.jar!/
        int bang = path.indexOf('!');
        if (bang >= 0) {
          path = path.substring(0, bang);
        }
        return new File(new URI(path)).getAbsolutePath();
      }
    } catch (Exception ignored) {
      // Malformed URL — skip silently
    }
    return null;
  }

  private void scanAlreadyLoadedClasses() {
    try {
      for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
        ClassLoader loader = cls.getClassLoader();
        if (loader == null) {
          continue; // bootstrap
        }
        enqueueFromProtectionDomain(loader, cls.getProtectionDomain());
      }
    } catch (Exception e) {
      log.log(Level.FINE, "SCA: error scanning already-loaded classes", e);
    }
  }

  // ---- Background processing ---------------------------------------------

  private void processQueue() {
    Logger otelLogger = openTelemetry.getLogsBridge().get("co.elastic.otel.sca");

    // Token-bucket style rate limiter: track the earliest time the next JAR may be emitted
    long nextEmitNanos = System.nanoTime();
    long intervalNanos =
        config.getJarsPerSecond() > 0 ? (1_000_000_000L / config.getJarsPerSecond()) : 0L;

    while (!stopped.get()) {
      PendingJar pending;
      try {
        pending = pendingJars.poll(1L, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      if (pending == null) {
        continue;
      }

      // Rate limit: wait until the next emission slot is available
      if (intervalNanos > 0) {
        long now = System.nanoTime();
        long delay = nextEmitNanos - now;
        if (delay > 0) {
          try {
            TimeUnit.NANOSECONDS.sleep(delay);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
        nextEmitNanos = Math.max(System.nanoTime(), nextEmitNanos) + intervalNanos;
      }

      processJar(pending, otelLogger);
    }

    // Drain remaining entries during shutdown
    PendingJar remaining;
    while ((remaining = pendingJars.poll()) != null) {
      processJar(remaining, otelLogger);
    }
    log.fine("SCA: processing thread stopped");
  }

  private void processJar(PendingJar pending, Logger otelLogger) {
    try {
      JarMetadata meta = JarMetadataExtractor.extract(pending.jarPath, pending.classloaderName);
      if (meta != null) {
        emitLogRecord(meta, otelLogger);
      }
    } catch (Exception e) {
      log.log(Level.FINE, "SCA: error processing JAR: " + pending.jarPath, e);
    }
  }

  private void emitLogRecord(JarMetadata meta, Logger otelLogger) {
    String body =
        meta.groupId.isEmpty()
            ? meta.name + ":" + meta.version
            : meta.groupId + ":" + meta.name + ":" + meta.version;

    otelLogger
        .logRecordBuilder()
        .setBody(body)
        .setAllAttributes(
            Attributes.builder()
                .put(ATTR_LIBRARY_NAME, meta.name)
                .put(ATTR_LIBRARY_VERSION, meta.version)
                .put(ATTR_LIBRARY_GROUP_ID, meta.groupId)
                .put(ATTR_LIBRARY_PURL, meta.purl)
                .put(ATTR_LIBRARY_JAR_PATH, meta.jarPath)
                .put(ATTR_LIBRARY_SHA256, meta.sha256)
                .put(ATTR_LIBRARY_CLASSLOADER, meta.classloaderName)
                .put(ATTR_EVENT_NAME, "library.loaded")
                .put(ATTR_EVENT_DOMAIN, "sca")
                .build())
        .emit();
  }

  // ---- Filtering ---------------------------------------------------------

  private boolean shouldSkip(String jarPath) {
    // Always skip the EDOT / upstream OTel agent JAR
    String fileName = new File(jarPath).getName();
    if (fileName.contains("elastic-otel-javaagent") || fileName.contains("opentelemetry-javaagent")) {
      return true;
    }
    if (agentJarPath != null && agentJarPath.equals(jarPath)) {
      return true;
    }
    // Skip temp JARs (e.g. JRuby, Groovy, or Spring Boot's exploded cache)
    if (config.isSkipTempJars()) {
      String normPath = normalise(jarPath);
      if (normPath.startsWith(tmpDir) || normPath.contains("/tmp/")) {
        return true;
      }
    }
    return false;
  }

  // ---- Utilities ---------------------------------------------------------

  /**
   * Best-effort: resolve the path of the agent JAR so we can exclude it from reporting. The test
   * harness in {@code custom} sets {@code elastic.otel.agent.jar.path}; in production we scan
   * the command line.
   */
  private static String resolveAgentJarPath() {
    String path = System.getProperty("elastic.otel.agent.jar.path");
    if (path != null) {
      return normalise(path);
    }
    // Fallback: parse -javaagent flag from the JVM command line
    String cmd = System.getProperty("sun.java.command", "");
    for (String token : cmd.split("\\s+")) {
      if (token.contains("elastic-otel-javaagent") || token.contains("opentelemetry-javaagent")) {
        return normalise(token);
      }
    }
    return null;
  }

  private static String normalise(String path) {
    return path.replace('\\', '/');
  }

  // ---- Inner types -------------------------------------------------------

  /** Lightweight holder placed in the pending queue. */
  private static final class PendingJar {
    final String jarPath;
    final String classloaderName;

    PendingJar(String jarPath, String classloaderName) {
      this.jarPath = jarPath;
      this.classloaderName = classloaderName;
    }
  }
}
