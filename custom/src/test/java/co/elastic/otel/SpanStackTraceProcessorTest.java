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

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;

import co.elastic.otel.common.ElasticAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpanStackTraceProcessorTest {

  InMemorySpanExporter spans;

  @BeforeEach
  void setUp() {
    spans = InMemorySpanExporter.create();
  }

  OpenTelemetrySdk createSdk(long minSpanDurationNanos) {
    SpanProcessor export = SimpleSpanProcessor.create(spans);
    SpanStackTraceProcessor proc = new SpanStackTraceProcessor(export, minSpanDurationNanos);

    SdkTracerProvider tracerProvider = SdkTracerProvider.builder().addSpanProcessor(proc).build();

    return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
  }

  @Test
  void checkStackTraceCorrect() {
    try (OpenTelemetrySdk sdk = createSdk(0L)) {
      Tracer tracer = sdk.getTracer("test-tracer");

      Span span = tracer.spanBuilder("my-span").startSpan();

      endCaller1(span);

      String framePrefix = "\tat " + getClass().getName();
      String expectedRegex =
          "(?s)"
              + Pattern.quote(framePrefix + ".endCaller2(")
              + "[^)]+\\)\\n"
              + Pattern.quote(framePrefix + ".endCaller1(")
              + "[^)]+\\)\\n.*";

      assertThat(spans.getFinishedSpanItems()).hasSize(1);
      assertThat(spans.getFinishedSpanItems().get(0))
          .hasName("my-span")
          .hasAttribute(
              satisfies(ElasticAttributes.SPAN_STACKTRACE, att -> att.matches(expectedRegex)));
    }
  }

  @Test
  void checkMinimumDurationRespected() {
    try (OpenTelemetrySdk sdk = createSdk(Long.MAX_VALUE)) {
      Tracer tracer = sdk.getTracer("test-tracer");

      tracer.spanBuilder("my-span").startSpan().end();

      assertThat(spans.getFinishedSpanItems()).hasSize(1);
      assertThat(spans.getFinishedSpanItems().get(0))
          .hasName("my-span")
          .hasAttributesSatisfying(
              attrib -> assertThat(attrib).doesNotContainKey(ElasticAttributes.SPAN_STACKTRACE));
    }
  }

  @Test
  void checkInferredSpansIgnored() {
    try (OpenTelemetrySdk sdk = createSdk(0L)) {
      Tracer tracer = sdk.getTracer("test-tracer");

      tracer
          .spanBuilder("my-span")
          .setAttribute(ElasticAttributes.IS_INFERRED, true)
          .startSpan()
          .end();

      assertThat(spans.getFinishedSpanItems()).hasSize(1);
      assertThat(spans.getFinishedSpanItems().get(0))
          .hasName("my-span")
          .hasAttributesSatisfying(
              attrib -> assertThat(attrib).doesNotContainKey(ElasticAttributes.SPAN_STACKTRACE));
    }
  }

  public void endCaller1(Span span) {
    endCaller2(span);
  }

  public void endCaller2(Span span) {
    span.end();
  }
}
