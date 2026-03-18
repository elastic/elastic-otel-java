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

import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the Elastic SCA extension.
 *
 * <p>Registered as both an {@link AutoConfigurationCustomizerProvider} and an {@link
 * AgentListener} via two {@code META-INF/services/} files so that it participates in the OTel
 * autoconfigure lifecycle at the correct phases:
 *
 * <ol>
 *   <li>{@link #customize} — called <em>before</em> the SDK is built; registers default values for
 *       all {@code elastic.otel.sca.*} config keys so they are visible to the OTel config pipeline.
 *   <li>{@link #afterAgent} — called <em>after</em> the SDK is fully initialised; reads the
 *       resolved configuration, obtains the JVM {@link Instrumentation} object, and starts the
 *       {@link JarCollectorService}.
 * </ol>
 *
 * <p>Following the pattern of {@code inferred-spans} / {@code ElasticAutoConfigurationCustomizerProvider}
 * + {@code ConfigLoggingAgentListener} in the {@code custom} module.
 */
public class SCAExtension implements AutoConfigurationCustomizerProvider, AgentListener {

  private static final Logger logger = Logger.getLogger(SCAExtension.class.getName());

  // ---- AutoConfigurationCustomizerProvider --------------------------------

  /**
   * Registers default values for {@code elastic.otel.sca.*} properties so that OTel's config
   * pipeline (system properties, env vars, SDK config file) can override them consistently.
   *
   * <p>Only sets a default when the user has not already supplied an explicit value, matching the
   * pattern used by {@code InferredSpansBackwardsCompatibilityConfig}.
   */
  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addPropertiesCustomizer(
        props -> {
          Map<String, String> defaults = new HashMap<>();
          setDefault(props, defaults, SCAConfiguration.ENABLED_KEY,
              Boolean.toString(SCAConfiguration.DEFAULT_ENABLED));
          setDefault(props, defaults, SCAConfiguration.SKIP_TEMP_JARS_KEY,
              Boolean.toString(SCAConfiguration.DEFAULT_SKIP_TEMP_JARS));
          setDefault(props, defaults, SCAConfiguration.JARS_PER_SECOND_KEY,
              Integer.toString(SCAConfiguration.DEFAULT_JARS_PER_SECOND));
          return defaults;
        });
  }

  private static void setDefault(
      io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties props,
      Map<String, String> defaults,
      String key,
      String defaultValue) {
    if (props.getString(key) == null) {
      defaults.put(key, defaultValue);
    }
  }

  // ---- AgentListener ------------------------------------------------------

  /**
   * Starts the {@link JarCollectorService} once the OTel SDK is fully initialised.
   *
   * <p>The JVM {@link Instrumentation} object is obtained via reflection from {@code
   * io.opentelemetry.javaagent.bootstrap.InstrumentationHolder}, which is the same internal holder
   * used by the upstream OTel Java agent. This class lives in the bootstrap classloader and is
   * accessible from the agent classloader at runtime without a compile-time dependency.
   */
  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    SCAConfiguration config = SCAConfiguration.get();

    if (!config.isEnabled()) {
      logger.fine("SCA extension is disabled via " + SCAConfiguration.ENABLED_KEY);
      return;
    }

    Instrumentation instrumentation = findInstrumentation();
    if (instrumentation == null) {
      logger.warning(
          "SCA: could not obtain JVM Instrumentation object — JAR scanning will not run");
      return;
    }

    JarCollectorService service =
        new JarCollectorService(
            autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk(), instrumentation, config);
    service.start();
  }

  /**
   * Retrieves the JVM {@link Instrumentation} instance from {@code
   * io.opentelemetry.javaagent.bootstrap.InstrumentationHolder}.
   *
   * <p>The OTel Java agent stores the {@code Instrumentation} it receives in {@code premain()} in
   * this bootstrap-classloader class. Because our code runs in the agent classloader (which
   * delegates to bootstrap), we can reach it via {@link Class#forName} without importing it at
   * compile time — and without using {@code ByteBuddyAgent.getInstrumentation()}.
   *
   * <p>This is the same mechanism used internally by the OTel contrib libraries (e.g.
   * {@code opentelemetry-javaagent-inferred-spans}) to access the {@link Instrumentation}.
   */
  static Instrumentation findInstrumentation() {
    try {
      Class<?> holder =
          Class.forName("io.opentelemetry.javaagent.bootstrap.InstrumentationHolder");
      Method getter = holder.getMethod("getInstrumentation");
      return (Instrumentation) getter.invoke(null);
    } catch (Exception e) {
      logger.log(
          Level.WARNING,
          "SCA: failed to obtain Instrumentation via InstrumentationHolder",
          e);
      return null;
    }
  }
}
