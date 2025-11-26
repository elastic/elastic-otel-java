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

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AgentFeaturesSmokeTest extends TestAppSmokeTest {

  @BeforeAll
  public static void start() {
    startTestApp(
        (container) -> container.addEnv("ELASTIC_OTEL_JAVA_SPAN_STACKTRACE_MIN_DURATION", "0ms"));
  }

  @AfterAll
  public static void end() {
    stopApp();
  }

  @Test
  public void healthcheck() throws InterruptedException {
    doRequest(getUrl("/health"), okResponseBody("Alive!"));

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans)
        .hasSize(1)
        .extracting("name", "kind")
        .containsOnly(tuple("GET /health", Span.SpanKind.SPAN_KIND_SERVER));

    spans.forEach(
        span -> {
          assertThat(getAttributes(span.getAttributesList()))
              .containsKeys("code.stacktrace");
        });
  }
}
