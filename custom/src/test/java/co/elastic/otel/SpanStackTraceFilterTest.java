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

import co.elastic.otel.common.ElasticAttributes;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
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

    SpanBuilder spanNotInferred =
        tracer.spanBuilder("span").setAttribute(ElasticAttributes.IS_INFERRED, Boolean.FALSE);
    assertThat(filter.test((ReadableSpan) spanNotInferred.startSpan())).isTrue();

    ReadableSpan inferredSpan =
        (ReadableSpan)
            tracer
                .spanBuilder("span")
                .setAttribute(ElasticAttributes.IS_INFERRED, Boolean.TRUE)
                .startSpan();
    assertThat(filter.test(inferredSpan)).describedAs("inferred spans must be filtered").isFalse();
  }
}
