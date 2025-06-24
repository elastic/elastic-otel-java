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
package co.elastic.otel.logging;

import static java.util.Collections.emptyList;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class AgentLog {

  /** Upstream instrumentation debug boolean option */
  public static final String OTEL_JAVAAGENT_DEBUG = "otel.javaagent.debug";

  private static final String PATTERN = "%d{DEFAULT} [%t] %-5level %logger{36} - %msg{nolookups}%n";

  /** root logger is an empty string */
  private static final String ROOT_LOGGER_NAME = "";

  /**
   * debug span logging exporter that can be controlled at runtime, only used when logging span
   * exporter has not been explicitly configured.
   */
  private static final DebugLogSpanExporter debugLogSpanExporter =
      new DebugLogSpanExporter(LoggingSpanExporter.create());

  private AgentLog() {}

  public static void init() {

    ConfigurationBuilder<BuiltConfiguration> conf =
        ConfigurationBuilderFactory.newConfigurationBuilder();

    conf.add(
        conf.newAppender("stdout", "Console")
            .add(conf.newLayout("PatternLayout").addAttribute("pattern", PATTERN)));

    conf.add(conf.newRootLogger().add(conf.newAppenderRef("stdout")));

    Configurator.initialize(conf.build(false));
  }

  public static void addSpanLoggingIfRequired(
      SdkTracerProviderBuilder providerBuilder, ConfigProperties config) {

    boolean otelDebug = config.getBoolean(OTEL_JAVAAGENT_DEBUG, false);

    // Replicate behavior of upstream agent: span logging exporter is automatically added
    // when not already present when debugging.
    // When logging exporter has been explicitly configured, spans logging will be done by the
    // explicitly configured logging exporter instance
    boolean loggingExporterNotAlreadyConfigured =
        !config.getList("otel.traces.exporter", emptyList()).contains("logging");
    if (otelDebug && loggingExporterNotAlreadyConfigured) {
      providerBuilder.addSpanProcessor(SimpleSpanProcessor.create(debugLogSpanExporter));
    }
  }

  public static void setLevel(String level) {
    switch (level) {
      case "trace":
        setLevel(Level.TRACE);
        return;
      case "debug":
        setLevel(Level.DEBUG);
        return;
      case "info":
        setLevel(Level.INFO);
        return;
      case "warn":
        setLevel(Level.WARN);
        return;
      case "error":
        setLevel(Level.ERROR);
        return;
      case "fatal":
        setLevel(Level.FATAL);
        return;
      case "off":
        setLevel(Level.OFF);
        return;
      default:
        setLevel(Level.INFO);
    }
  }

  /**
   * Sets the agent log level at runtime
   *
   * @param level log level
   */
  public static void setLevel(Level level) {
    // Using log4j2 implementation allows to change the log level programmatically at runtime
    // which is not directly possible through the slf4j API and simple implementation used in
    // upstream distribution.

    Configurator.setAllLevels(ROOT_LOGGER_NAME, level);

    boolean isDebug = level.intLevel() >= Level.DEBUG.intLevel();

    // When debugging, we should avoid very chatty http client debug messages
    // this behavior is replicated from the upstream distribution.
    if (isDebug) {
      Configurator.setLevel("okhttp3.internal.http2", Level.INFO);
      Configurator.setLevel("okhttp3.internal.concurrent.TaskRunner", Level.INFO);
    }

    // when debugging the upstream otel agent configures an extra debug exporter
    debugLogSpanExporter.setEnabled(isDebug);
  }

  private static class DebugLogSpanExporter implements SpanExporter {

    private final SpanExporter delegate;
    private final AtomicBoolean enabled;

    DebugLogSpanExporter(SpanExporter delegate) {
      this.delegate = delegate;
      this.enabled = new AtomicBoolean(false);
    }

    void setEnabled(boolean value) {
      enabled.set(value);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      return enabled.get() ? delegate.export(spans) : CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
      return enabled.get() ? delegate.flush() : CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
      return enabled.get() ? delegate.shutdown() : CompletableResultCode.ofSuccess();
    }
  }
}
