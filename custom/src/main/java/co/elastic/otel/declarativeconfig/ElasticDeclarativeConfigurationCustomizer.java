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

import static co.elastic.otel.ElasticUserAgentHeader.OTLP_GRPC;
import static co.elastic.otel.ElasticUserAgentHeader.OTLP_HTTP;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static jdk.internal.net.http.HttpRequestImpl.USER_AGENT;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectionModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ResourceModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class ElasticDeclarativeConfigurationCustomizer
    implements DeclarativeConfigurationCustomizerProvider {

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          customizeResources(model);
          customizeUserAgent(model);
          return model;
        });
  }

  private static void customizeResources(OpenTelemetryConfigurationModel model) {
    // this is equivalent to adding the following explicitly in declarative configuration
    //
    //  detection/development:
    //    detectors:
    //      - <... other detectors ...>
    //      - elastic_distribution:

    ResourceModel resource = model.getResource();
    if (resource == null) {
      resource = new ResourceModel();
      model.withResource(resource);
    }

    ExperimentalResourceDetectionModel detectionDevelopment = resource.getDetectionDevelopment();
    if (detectionDevelopment == null) {
      detectionDevelopment = new ExperimentalResourceDetectionModel();
      resource.withDetectionDevelopment(detectionDevelopment);
    }
    List<ExperimentalResourceDetectorModel> detectors =
        requireNonNull(detectionDevelopment.getDetectors());

    Set<String> names =
        detectors.stream()
            .flatMap(detector -> detector.getAdditionalProperties().keySet().stream())
            .collect(toSet());

    // add at the end to make it have priority over upstream distro provider (which is added 1st)
    if (!names.contains(ElasticDistroComponentProvider.NAME)) {
      ExperimentalResourceDetectorModel detector = new ExperimentalResourceDetectorModel();
      detector.getAdditionalProperties().put(ElasticDistroComponentProvider.NAME, null);
      detectors.add(detector);
    }
  }

  private void customizeUserAgent(OpenTelemetryConfigurationModel model) {
    // configure otlp exporters (grpc or http) to set the user-agent if not explicitly set
    //
    // This is equivalent to explicitly add 'user-agent' header in exporter configuration
    // for 'tracer_provider', 'meter_provider' and 'logger_provider'.
    //
    // tracer_provider:
    //   processors:
    //     # or 'simple' for simple span processor
    //     - batch:
    //         exporter:
    //           # or 'otlp_grpc' for grpc
    //           otlp_http:
    //             endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4318}/v1/traces
    //             headers:
    //               # with 'elastic-otlp-grpc-java' prefix for grpc
    //               user-agent: "elastic-otlp-http-java/<edot-version>"
    //
    // meter_provider:
    //   readers:
    //     - periodic:
    //         exporter:
    //           # or 'otlp_grpc' for grpc
    //           otlp_http:
    //             endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4318}/v1/metrics
    //             headers:
    //               # with 'elastic-otlp-grpc-java' prefix for grpc
    //               user-agent: "elastic-otlp-http-java/<edot-version>"
    //
    // logger_provider:
    //   processors:
    //     # or 'simple' for simple log processor
    //     - batch:
    //         exporter:
    //           otlp_http:
    //             endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4318}/v1/logs
    //             headers:
    //               # with 'elastic-otlp-grpc-java' prefix for grpc
    //               user-agent: "elastic-otlp-http-java/<edot-version>"

    // traces
    Optional.ofNullable(model.getTracerProvider())
        .map(TracerProviderModel::getProcessors)
        .orElse(Collections.emptyList())
        .forEach(
            processor -> {
              Optional.ofNullable(processor.getBatch())
                  .map(BatchSpanProcessorModel::getExporter)
                  .ifPresent(this::addUserAgent);
              Optional.ofNullable(processor.getSimple())
                  .map(SimpleSpanProcessorModel::getExporter)
                  .ifPresent(this::addUserAgent);
            });

    // metrics
    Optional.ofNullable(model.getMeterProvider())
        .map(MeterProviderModel::getReaders)
        .orElse(Collections.emptyList())
        .forEach(
            reader -> {
              Optional.ofNullable(reader.getPeriodic())
                  .map(PeriodicMetricReaderModel::getExporter)
                  .ifPresent(this::addUserAgent);
            });

    // logs
    Optional.ofNullable(model.getLoggerProvider())
        .map(LoggerProviderModel::getProcessors)
        .orElse(Collections.emptyList())
        .forEach(
            processor -> {
              Optional.ofNullable(processor.getBatch())
                  .map(BatchLogRecordProcessorModel::getExporter)
                  .ifPresent(this::addUserAgent);
              Optional.ofNullable(processor.getSimple())
                  .map(SimpleLogRecordProcessorModel::getExporter)
                  .ifPresent(this::addUserAgent);
            });
  }

  private void addUserAgent(@Nullable LogRecordExporterModel logExporterModel) {
    if (logExporterModel == null) {
      return;
    }
    addUserAgent(logExporterModel.getOtlpGrpc(), logExporterModel.getOtlpHttp());
  }

  private void addUserAgent(@Nullable PushMetricExporterModel metricExporterModel) {
    if (metricExporterModel == null) {
      return;
    }

    OtlpGrpcMetricExporterModel otlpGrpc = metricExporterModel.getOtlpGrpc();
    if (otlpGrpc != null) {
      String headersList = otlpGrpc.getHeadersList();
      List<NameStringValuePairModel> headers = otlpGrpc.getHeaders();
      otlpGrpc.withHeaders(addUserAgent(headersList, headers, OTLP_GRPC));
    }
    OtlpHttpMetricExporterModel otlpHttp = metricExporterModel.getOtlpHttp();
    if (otlpHttp != null) {
      String headersList = otlpHttp.getHeadersList();
      List<NameStringValuePairModel> headers = otlpHttp.getHeaders();
      otlpHttp.withHeaders(addUserAgent(headersList, headers, OTLP_HTTP));
    }
  }

  private void addUserAgent(@Nullable SpanExporterModel spanExporterModel) {
    if (spanExporterModel == null) {
      return;
    }
    addUserAgent(spanExporterModel.getOtlpGrpc(), spanExporterModel.getOtlpHttp());
  }

  private static void addUserAgent(OtlpGrpcExporterModel otlpGrpc, OtlpHttpExporterModel otlpHttp) {
    if (otlpGrpc != null) {
      String headersList = otlpGrpc.getHeadersList();
      List<NameStringValuePairModel> headers = otlpGrpc.getHeaders();
      otlpGrpc.withHeaders(addUserAgent(headersList, headers, OTLP_GRPC));
    }
    if (otlpHttp != null) {
      String headersList = otlpHttp.getHeadersList();
      List<NameStringValuePairModel> headers = otlpHttp.getHeaders();
      otlpHttp.withHeaders(addUserAgent(headersList, headers, OTLP_HTTP));
    }
  }

  // package protected for testing
  static List<NameStringValuePairModel> addUserAgent(
      @Nullable String headersList, List<NameStringValuePairModel> headers, String value) {
    // skip if user-agent is already present in headers list (string)
    if (headersList != null) {
      for (String part : headersList.split(",")) {
        String[] keyValue = part.split("=", 2);
        if (keyValue.length == 2 && USER_AGENT.equalsIgnoreCase(keyValue[0].trim())) {
          return headers;
        }
      }
    }
    // skip if user-agent is already present in headers (list)
    if (headers != null && !headers.isEmpty()) {
      for (NameStringValuePairModel header : headers) {
        if (USER_AGENT.equalsIgnoreCase(header.getName())) {
          return headers;
        }
      }
    }

    List<NameStringValuePairModel> result = new ArrayList<>();
    if (headers != null) {
      result.addAll(headers);
    }
    result.add(new NameStringValuePairModel().withName(USER_AGENT).withValue(value));
    return result;
  }
}
