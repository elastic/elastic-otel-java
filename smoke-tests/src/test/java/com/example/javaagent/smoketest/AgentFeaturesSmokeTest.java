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
package com.example.javaagent.smoketest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.google.protobuf.Any;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AgentFeaturesSmokeTest extends TestAppSmokeTest {

  @BeforeAll
  public static void start() {
    startTestApp(
        (container) -> {
          // capture span stacktrace for any duration
          container.addEnv("ELASTIC_OTEL_JAVA_SPAN_STACKTRACE_MIN_DURATION", "0ms");
          // capture HTTP request/response headers on server side
          // header key should not be case-sensitive in config
          container.addEnv("OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_REQUEST_HEADERS", "hello");
          container.addEnv("OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_RESPONSE_HEADERS", "Content-length,Date");
        }
    );
  }

  // TODO remove useless public modifiers ?

  @AfterAll
  public static void end() {
    stopApp();
  }

  @Test
  public void spanCodeStackTrace() {
    doRequest(getUrl("/health"), okResponseBody("Alive!"));

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans)
        .hasSize(1)
        .extracting("name", "kind")
        .containsOnly(tuple("GET /health", Span.SpanKind.SPAN_KIND_SERVER));

    spans.forEach(
        span -> assertThat(getAttributes(span.getAttributesList()))
            .containsKeys("code.stacktrace"));
  }

  @Test
  public void httpHeaderCapture() {
    Map<String, String> headers = new HashMap<>();
    headers.put("Hello", "World!");
    doRequest(getUrl("/health"), headers, okResponseBody("Alive!"));

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans)
        .hasSize(1)
        .extracting("name", "kind")
        .containsOnly(tuple("GET /health", Span.SpanKind.SPAN_KIND_SERVER));

    spans.forEach(span -> assertThat(getAttributes(span.getAttributesList()))
        .containsEntry("http.request.header.hello", attributeArrayValue("World!"))
        .containsEntry("http.response.header.content-length", attributeArrayValue("6"))
        .containsKey("http.response.header.date"));

  }
}
