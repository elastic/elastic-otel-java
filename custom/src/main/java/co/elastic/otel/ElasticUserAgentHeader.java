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

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.javaagent.tooling.AgentVersion;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class ElasticUserAgentHeader {

  private static final String HEADER_NAME = "User-Agent";
  private static final String GRPC_VALUE = "elastic-otlp-grpc-java/" + AgentVersion.VERSION;
  private static final String HTTP_VALUE = "elastic-otlp-http-java/" + AgentVersion.VERSION;

  public static SpanExporter configureIfPossible(SpanExporter spanExporter) {
    if (spanExporter instanceof OtlpGrpcSpanExporter) {
      return ((OtlpGrpcSpanExporter) spanExporter)
          .toBuilder().addHeader(HEADER_NAME, GRPC_VALUE).build();
    } else if (spanExporter instanceof OtlpHttpSpanExporter) {
      return ((OtlpHttpSpanExporter) spanExporter)
          .toBuilder().addHeader(HEADER_NAME, HTTP_VALUE).build();
    }
    return spanExporter;
  }

  public static MetricExporter configureIfPossible(MetricExporter metricExporter) {
    if (metricExporter instanceof OtlpGrpcMetricExporter) {
      return ((OtlpGrpcMetricExporter) metricExporter)
          .toBuilder().addHeader(HEADER_NAME, GRPC_VALUE).build();
    } else if (metricExporter instanceof OtlpHttpMetricExporter) {
      return ((OtlpHttpMetricExporter) metricExporter)
          .toBuilder().addHeader(HEADER_NAME, HTTP_VALUE).build();
    }
    return metricExporter;
  }

  public static LogRecordExporter configureIfPossible(LogRecordExporter logExporter) {
    if (logExporter instanceof OtlpGrpcLogRecordExporter) {
      return ((OtlpGrpcLogRecordExporter) logExporter)
          .toBuilder().addHeader(HEADER_NAME, GRPC_VALUE).build();
    } else if (logExporter instanceof OtlpHttpLogRecordExporter) {
      return ((OtlpHttpLogRecordExporter) logExporter)
          .toBuilder().addHeader(HEADER_NAME, HTTP_VALUE).build();
    }
    return logExporter;
  }
}
