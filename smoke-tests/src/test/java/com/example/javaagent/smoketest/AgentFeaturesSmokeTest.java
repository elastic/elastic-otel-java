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
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AgentFeaturesSmokeTest extends TestAppSmokeTest {

  @BeforeAll
  public static void start() {
    startApp();
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
        .hasSize(2)
        .extracting("name", "kind")
        .containsOnly(
            tuple("GET /health", Span.SpanKind.SPAN_KIND_SERVER),
            tuple("HealthController.healthcheck", Span.SpanKind.SPAN_KIND_INTERNAL));

    spans.forEach(
        span -> {
          assertThat(getAttributes(span.getAttributesList()))
              .containsKeys(
                  "elastic.span.is_local_root",
                  "elastic.span.local_root.id",
                  "elastic.span.self_time");
        });
  }

  @Test
  void profiling1() throws IOException, InterruptedException {
    profilingScenario(1, 3);
  }

  @Test
  void profiling2() throws IOException, InterruptedException {
    profilingScenario(2, 6);
  }

  @Test
  void profiling3() throws IOException, InterruptedException {
    profilingScenario(3, 10);
  }

  @Test
  void profiling4() throws IOException, InterruptedException {
    profilingScenario(4, 3);
  }

  private void profilingScenario(int id, int expectedRegularSpans)
      throws IOException, InterruptedException {
    List<Span> spans = profilingScenario(id);
    Span rootSpan =
        spans.stream()
            .filter(span -> span.getKind().equals(Span.SpanKind.SPAN_KIND_SERVER))
            .findFirst()
            .orElseThrow();

    assertThat(getAttributes(rootSpan.getAttributesList()))
        .containsEntry("elastic.span.is_local_root", attributeValue(true))
        .containsEntry(
            "elastic.span.local_root.id",
            attributeValue(bytesToHex(rootSpan.getSpanId().toByteArray())));

    List<Span> inferred =
        spans.stream().filter(span -> span.getName().startsWith("inferred")).toList();

    inferred.forEach(
        span -> {
          assertThat(getAttributes(span.getAttributesList()))
              .containsKey("elastic.span.inferred_samples");
        });

    List<Span> regularSpans =
        spans.stream().filter(span -> !span.getName().startsWith("inferred")).toList();

    assertThat(
            regularSpans.stream()
                .map(s -> s.getName() + " " + bytesToHex(s.getSpanId().toByteArray())))
        .hasSize(expectedRegularSpans);

    regularSpans.stream()
        .filter(span -> !span.getSpanId().equals(rootSpan.getSpanId()))
        .forEach(
            span -> {
              assertThat(getAttributes(span.getAttributesList()))
                  .containsEntry("elastic.span.is_local_root", attributeValue(false))
                  .containsEntry(
                      "elastic.span.local_root.id",
                      attributeValue(bytesToHex(rootSpan.getSpanId().toByteArray())));
            });
  }

  private List<Span> profilingScenario(int id) {
    doRequest(
        getUrl("/profiling/scenario/" + id), okResponseBody(String.format("scenario %d OK", id)));

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).collect(Collectors.toList());
    assertThat(spans)
        .extracting("name", "kind")
        .contains(
            tuple("GET /profiling/scenario/{id}", Span.SpanKind.SPAN_KIND_SERVER),
            tuple("ProfilingController.scenario", Span.SpanKind.SPAN_KIND_INTERNAL));

    return spans;
  }
}
