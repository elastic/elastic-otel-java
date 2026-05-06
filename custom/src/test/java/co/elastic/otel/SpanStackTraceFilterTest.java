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

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.inferredspans.InferredSpansProcessor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.jupiter.api.Test;

class SpanStackTraceFilterTest {

  @Test
  void filtering() {
    Tracer tracer =
        OpenTelemetrySdk.builder().build().getTracerProvider().tracerBuilder("test").build();

    SpanStackTraceFilter filter = new SpanStackTraceFilter();

    ReadableSpan simpleSpan = (ReadableSpan) tracer.spanBuilder("span").startSpan();
    assertThat(filter.test(simpleSpan)).isTrue();

    ReadableSpan spanNotInferred =
        (ReadableSpan)
            tracer
                .spanBuilder("span")
                .startSpan();
    assertThat(filter.test(spanNotInferred)).isTrue();

    Tracer inferredSpanTracer =
        OpenTelemetrySdk.builder()
            .build()
            .getTracerProvider()
            .tracerBuilder(InferredSpansProcessor.TRACER_NAME)
            .build();

    ReadableSpan inferredSpanTracerSpan =
        (ReadableSpan) inferredSpanTracer.spanBuilder("span").startSpan();
    assertThat(filter.test(inferredSpanTracerSpan))
        .describedAs("spans from inferred-spans tracer must be filtered")
        .isFalse();
  }
}
