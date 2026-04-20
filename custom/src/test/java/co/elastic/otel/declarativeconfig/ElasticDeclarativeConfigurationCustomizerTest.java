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
package co.elastic.otel.declarativeconfig;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.tooling.AgentVersion;
import io.opentelemetry.javaagent.tooling.resources.ResourceCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ElasticDeclarativeConfigurationCustomizerTest {

  @Test
  void defaultConfig() {
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    model = applyConfigCustomize(model, new ElasticDeclarativeConfigurationCustomizer());

    // ensures that we add our resource detector even if the model does not provide any
    assertThatJson(json(model.getResource())).inPath("attributes").isArray().isEmpty();
    assertThatJson(json(model.getResource()))
        .inPath("detection/development.detectors")
        .isArray()
        .containsExactly(json("{\"elastic_distribution\":null}]}"));

    // no exporter is configured by default for any signal
    assertThat(model.getTracerProvider()).isNull();
    assertThat(model.getMeterProvider()).isNull();
    assertThat(model.getLoggerProvider()).isNull();
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void distributionResourceProvider(boolean elasticFirst) {
    // upstream provider is always added first in the list, even if we add ours first
    // this ordering behavior is implemented in upstream provider
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    DeclarativeConfigurationCustomizerProvider first;
    DeclarativeConfigurationCustomizerProvider second;
    if (elasticFirst) {
      first = new ElasticDeclarativeConfigurationCustomizer();
      second = new ResourceCustomizerProvider();
    } else {
      first = new ElasticDeclarativeConfigurationCustomizer();
      second = new ResourceCustomizerProvider();
    }

    model = applyConfigCustomize(model, first);
    model = applyConfigCustomize(model, second);

    assertThatJson(json(model.getResource()))
        .inPath("detection/development.detectors")
        .isArray()
        .containsExactly(
            json("{\"opentelemetry_javaagent_distribution\":null}]}"),
            json("{\"elastic_distribution\":null}]}"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"grpc", "http"})
  void userAgent(String protocol) {
    // setup tracer provider with single + batch exporter
    // check that the user-agent value is set as expected
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();

    SpanExporterModel spanExporter = new SpanExporterModel();
    PushMetricExporterModel metricExporter = new PushMetricExporterModel();
    LogRecordExporterModel logExporter = new LogRecordExporterModel();

    switch (protocol) {
      case "grpc":
        spanExporter.withOtlpGrpc(new OtlpGrpcExporterModel());
        metricExporter.withOtlpGrpc(new OtlpGrpcMetricExporterModel());
        logExporter.withOtlpGrpc(new OtlpGrpcExporterModel());
        break;
      case "http":
        spanExporter.withOtlpHttp(new OtlpHttpExporterModel());
        metricExporter.withOtlpHttp(new OtlpHttpMetricExporterModel());
        logExporter.withOtlpHttp(new OtlpHttpExporterModel());
        break;
      default:
        throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }

    // tracer provider
    SpanProcessorModel spanSimpleProcessor =
        new SpanProcessorModel()
            .withSimple(new SimpleSpanProcessorModel().withExporter(spanExporter));
    SpanProcessorModel spanBatchProcessor =
        new SpanProcessorModel()
            .withBatch(new BatchSpanProcessorModel().withExporter(spanExporter));
    model.withTracerProvider(
        new TracerProviderModel()
            .withProcessors(Arrays.asList(spanSimpleProcessor, spanBatchProcessor)));

    // meter provider
    model.withMeterProvider(
        new MeterProviderModel()
            .withReaders(
                Collections.singletonList(
                    new MetricReaderModel()
                        .withPeriodic(
                            new PeriodicMetricReaderModel().withExporter(metricExporter)))));

    // logger provider
    LogRecordProcessorModel logSimpleProcessor =
        new LogRecordProcessorModel()
            .withSimple(new SimpleLogRecordProcessorModel().withExporter(logExporter));
    LogRecordProcessorModel logBatchProcessor =
        new LogRecordProcessorModel()
            .withBatch(new BatchLogRecordProcessorModel().withExporter(logExporter));
    model.withLoggerProvider(
        new LoggerProviderModel()
            .withProcessors(Arrays.asList(logSimpleProcessor, logBatchProcessor)));

    model = applyConfigCustomize(model, new ElasticDeclarativeConfigurationCustomizer());

    // tracer provider
    assertThat(model.getTracerProvider()).isNotNull();
    assertThatJson(model.getTracerProvider()).inPath("processors").isArray().hasSize(2);
    for (int i = 0; i < 2; i++) {
      String pathPrefix = i == 0 ? "simple" : "batch";
      assertThatJson(model.getTracerProvider().getProcessors().get(i))
          .inPath(pathPrefix + ".exporter.otlp_" + protocol + ".headers")
          .isArray()
          .contains(userAgentHeader("elastic-otlp-" + protocol + "-java/" + AgentVersion.VERSION));
    }

    // meter provider
    assertThat(model.getMeterProvider()).isNotNull();
    assertThatJson(model.getMeterProvider()).inPath("readers").isArray().hasSize(1);
    assertThatJson(model.getMeterProvider().getReaders().get(0))
        .inPath("periodic.exporter.otlp_" + protocol + ".headers")
        .isArray()
        .contains(userAgentHeader("elastic-otlp-" + protocol + "-java/" + AgentVersion.VERSION));

    // logger provider
    assertThat(model.getLoggerProvider()).isNotNull();
    assertThatJson(model.getLoggerProvider()).inPath("processors").isArray().hasSize(2);
    for (int i = 0; i < 2; i++) {
      String pathPrefix = i == 0 ? "simple" : "batch";
      assertThatJson(model.getLoggerProvider().getProcessors().get(i))
          .inPath(pathPrefix + ".exporter.otlp_" + protocol + ".headers")
          .isArray()
          .contains(userAgentHeader("elastic-otlp-" + protocol + "-java/" + AgentVersion.VERSION));
    }
  }

  @Test
  void addUserAgentPriority() {
    assertThat(ElasticDeclarativeConfigurationCustomizer.addUserAgent(null, null, "my-user-agent"))
        .describedAs("add user agent when none is present in headers list or headers")
        .hasSize(1)
        .flatExtracting("name", "value")
        .contains("User-Agent", "my-user-agent");

    assertThat(
            ElasticDeclarativeConfigurationCustomizer.addUserAgent(
                null, Collections.emptyList(), "my-user-agent"))
        .describedAs("add user agent when none is present in headers list or headers")
        .hasSize(1)
        .flatExtracting("name", "value")
        .contains("User-Agent", "my-user-agent");

    assertThat(
            ElasticDeclarativeConfigurationCustomizer.addUserAgent(
                // using non-standard case is intentional for testing
                "User-agent=custom-user-agent", null, "my-user-agent"))
        .describedAs("configured user-agent is preserved")
        .isNull();

    assertThat(
            ElasticDeclarativeConfigurationCustomizer.addUserAgent(
                null,
                Collections.singletonList(
                    new NameStringValuePairModel()
                        .withName("user-agent") // using lower-case is intentional for testing
                        .withValue("custom-user-Agent")),
                "my-user-agent"))
        .describedAs("configured user-agent is preserved")
        .hasSize(1)
        .flatExtracting("name", "value")
        .contains("user-agent", "custom-user-Agent");
  }

  @NotNull
  private static Map<String, String> userAgentHeader(String value) {
    Map<String, String> header = new HashMap<>();
    header.put("name", "User-Agent");
    header.put("value", value);
    return header;
  }

  @ParameterizedTest
  @MethodSource("metricExporterTemporalityValues")
  void metricExporterTemporality(String protocol, @Nullable String userSetTemporality) {

    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    PushMetricExporterModel metricExporterModel = new PushMetricExporterModel();

    switch (protocol) {
      case "grpc":
        OtlpHttpMetricExporterModel.ExporterTemporalityPreference grpcUserPreference = null;
        if (userSetTemporality != null) {
          grpcUserPreference =
              OtlpHttpMetricExporterModel.ExporterTemporalityPreference.fromValue(
                  userSetTemporality);
        }
        metricExporterModel.withOtlpGrpc(
            new OtlpGrpcMetricExporterModel().withTemporalityPreference(grpcUserPreference));
        break;
      case "http":
        OtlpHttpMetricExporterModel.ExporterTemporalityPreference httpUserPreference = null;
        if (userSetTemporality != null) {
          httpUserPreference =
              OtlpHttpMetricExporterModel.ExporterTemporalityPreference.fromValue(
                  userSetTemporality);
        }
        metricExporterModel.withOtlpHttp(
            new OtlpHttpMetricExporterModel().withTemporalityPreference(httpUserPreference));
        break;
      default:
        throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }

    model.withMeterProvider(
        new MeterProviderModel()
            .withReaders(
                Collections.singletonList(
                    new MetricReaderModel()
                        .withPeriodic(
                            new PeriodicMetricReaderModel().withExporter(metricExporterModel)))));

    applyConfigCustomize(model, new ElasticDeclarativeConfigurationCustomizer());

    assertThat(model.getMeterProvider()).isNotNull();
    assertThat(model.getMeterProvider().getReaders()).hasSize(1);
    assertThatJson(json(model.getMeterProvider().getReaders().get(0)))
        .inPath("periodic.exporter.otlp_" + protocol + ".temporality_preference")
        .isEqualTo(userSetTemporality != null ? userSetTemporality : "delta");
  }

  public static Stream<Arguments> metricExporterTemporalityValues() {
    ArrayList<Arguments> args = new ArrayList<>();
    for (String protocol : Arrays.asList("grpc", "http")) {
      for (OtlpHttpMetricExporterModel.ExporterTemporalityPreference temporality :
          OtlpHttpMetricExporterModel.ExporterTemporalityPreference.values()) {
        String userValue = temporality.name().toLowerCase();
        args.add(Arguments.of(protocol, userValue));
      }
      // test for default value when user does not set any value
      args.add(Arguments.of(protocol, null));
    }
    return args.stream();
  }

  private OpenTelemetryConfigurationModel applyConfigCustomize(
      OpenTelemetryConfigurationModel originalModel,
      DeclarativeConfigurationCustomizerProvider customizerProvider) {
    AtomicReference<OpenTelemetryConfigurationModel> resultModel = new AtomicReference<>();
    customizerProvider.customize(
        new TestCustomizer() {
          @Override
          public void addModelCustomizer(
              Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel>
                  customizer) {
            resultModel.set(customizer.apply(originalModel));
          }
        });
    return resultModel.get();
  }

  private static class TestCustomizer implements DeclarativeConfigurationCustomizer {

    @Override
    public void addModelCustomizer(
        Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel> customizer) {}

    @Override
    public <T extends SpanExporter> void addSpanExporterCustomizer(
        Class<T> exporterType, BiFunction<T, DeclarativeConfigProperties, T> customizer) {}

    @Override
    public <T extends MetricExporter> void addMetricExporterCustomizer(
        Class<T> exporterType, BiFunction<T, DeclarativeConfigProperties, T> customizer) {}

    @Override
    public <T extends LogRecordExporter> void addLogRecordExporterCustomizer(
        Class<T> exporterType, BiFunction<T, DeclarativeConfigProperties, T> customizer) {}
  }
}
