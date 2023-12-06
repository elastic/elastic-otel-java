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
import static org.mockito.Mockito.mock;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ElasticSpanProcessorTest {

  private static final Tracer tracer;
  private static final InMemorySpanExporter testExporter;

  static {
    ElasticSpanProcessor elasticSpanProcessor =
        new ElasticSpanProcessor(mock(ElasticProfiler.class), mock(ElasticBreakdownMetrics.class));

    testExporter = InMemorySpanExporter.create();
    ElasticSpanExporter elasticSpanExporter = new ElasticSpanExporter(testExporter);
    elasticSpanProcessor.registerSpanExporter(elasticSpanExporter);

    tracer =
        SdkTracerProvider.builder()
            .addSpanProcessor(elasticSpanProcessor)
            .addSpanProcessor(SimpleSpanProcessor.create(elasticSpanExporter))
            .build()
            .get("for-testing");
  }

  @BeforeEach
  public void before() {
    testExporter.reset();
  }

  @Test
  void spanStackTraceCapture() {
    tracer.spanBuilder("span").startSpan().end();

    assertThat(testExporter.getFinishedSpanItems())
        .hasSize(1)
        .first()
        .satisfies(
            spanData ->
                assertThat(spanData)
                    .hasAttributesSatisfying(
                        satisfies(
                            ElasticAttributes.SPAN_STACKTRACE,
                            AbstractCharSequenceAssert::isNotEmpty)));
  }

  @Test
  void spanStackTraceCaptureDoesNotOverwrite() {
    String value = "dummy";
    tracer
        .spanBuilder("span")
        .setAttribute(ElasticAttributes.SPAN_STACKTRACE, value)
        .startSpan()
        .end();

    assertThat(testExporter.getFinishedSpanItems())
        .hasSize(1)
        .first()
        .satisfies(
            spanData ->
                assertThat(spanData).hasAttribute(ElasticAttributes.SPAN_STACKTRACE, value));
  }
}
