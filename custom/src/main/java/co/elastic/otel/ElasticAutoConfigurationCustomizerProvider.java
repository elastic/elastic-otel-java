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
package co.elastic.otel;

import co.elastic.otel.dynamicconfig.BlockableLogRecordExporter;
import co.elastic.otel.dynamicconfig.BlockableMetricExporter;
import co.elastic.otel.dynamicconfig.BlockableSpanExporter;
import co.elastic.otel.dynamicconfig.DynamicConfiguration;
import co.elastic.otel.dynamicconfig.DynamicInstrumentation;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ElasticAutoConfigurationCustomizerProvider
    implements AutoConfigurationCustomizerProvider {

  private static final String DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";
  private static final String RUNTIME_EXPERIMENTAL_TELEMETRY =
      "otel.instrumentation.runtime-telemetry.emit-experimental-telemetry";
  private static final String METRIC_TEMPORALITY_PREFERENCE =
      "otel.exporter.otlp.metrics.temporality.preference";


  // must match value in io.opentelemetry.contrib.stacktrace.StackTraceAutoConfig
  private static final String STACKTRACE_OTEL_FILTER =
      "otel.java.experimental.span-stacktrace.filter";
  static final String STACKTRACE_OTEL_DURATION =
      "otel.java.experimental.span-stacktrace.min.duration";
  static final String STACKTRACE_LEGACY1_DURATION =
      "elastic.otel.java.span-stacktrace.min.duration";
  static final String STACKTRACE_LEGACY2_DURATION =
      "elastic.otel.java.span.stacktrace.min.duration";

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addMetricExporterCustomizer(
        (metricexporter, configProperties) ->
            BlockableMetricExporter.createCustomInstance(metricexporter));
    autoConfiguration.addSpanExporterCustomizer(
        (spanExporter, configProperties) ->
            BlockableSpanExporter.createCustomInstance(spanExporter));
    autoConfiguration.addLogRecordExporterCustomizer(
        (logExporter, configProperties) ->
            BlockableLogRecordExporter.createCustomInstance(logExporter));

    autoConfiguration.addPropertiesCustomizer(
        ElasticAutoConfigurationCustomizerProvider::propertiesCustomizer);
    autoConfiguration.addTracerProviderCustomizer(
        (providerBuilder, properties) -> {
          DynamicInstrumentation.setTracerConfigurator(
              providerBuilder, DynamicConfiguration.UpdatableConfigurator.INSTANCE);
          return providerBuilder;
        });
  }

  static Map<String, String> propertiesCustomizer(ConfigProperties configProperties) {
    Map<String, String> config = new HashMap<>();

    experimentalTelemetry(config, configProperties);
    deltaMetricsTemporality(config, configProperties);
    resourceProviders(config, configProperties);
    spanStackTrace(config, configProperties);

    return config;
  }

  private static void experimentalTelemetry(
      Map<String, String> config, ConfigProperties configProperties) {
    // enable experimental telemetry metrics by default if not explicitly disabled
    boolean experimentalTelemetry =
        configProperties.getBoolean(RUNTIME_EXPERIMENTAL_TELEMETRY, true);
    config.put(RUNTIME_EXPERIMENTAL_TELEMETRY, Boolean.toString(experimentalTelemetry));
  }

  private static void deltaMetricsTemporality(
      Map<String, String> config, ConfigProperties configProperties) {
    // enable experimental telemetry metrics by default if not explicitly disabled
    String temporalityPreference =
        configProperties.getString(METRIC_TEMPORALITY_PREFERENCE, "DELTA");
    config.put(METRIC_TEMPORALITY_PREFERENCE, temporalityPreference);
  }

  private static void resourceProviders(
      Map<String, String> config, ConfigProperties configProperties) {
    Set<String> disabledResourceProviders =
        new HashSet<>(configProperties.getList(DISABLED_RESOURCE_PROVIDERS));

    // disable upstream distro name & version provider
    disabledResourceProviders.add(
        "io.opentelemetry.javaagent.tooling.DistroVersionResourceProvider");
    config.put(DISABLED_RESOURCE_PROVIDERS, String.join(",", disabledResourceProviders));
  }

  private static void spanStackTrace(
      Map<String, String> config, ConfigProperties configProperties) {

    String value = configProperties.getString(STACKTRACE_OTEL_DURATION);
    if (value == null) {
      value = configProperties.getString(STACKTRACE_LEGACY1_DURATION);
      if (value == null) {
        value = configProperties.getString(STACKTRACE_LEGACY2_DURATION);
      }
      config.put(STACKTRACE_OTEL_DURATION, value);
    }

    config.put(STACKTRACE_OTEL_FILTER, SpanStackTraceFilter.class.getName());
  }
}
