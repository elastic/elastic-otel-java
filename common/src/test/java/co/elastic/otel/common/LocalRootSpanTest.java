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
package co.elastic.otel.common;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import co.elastic.otel.testing.MapGetter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LocalRootSpanTest {

  private static OpenTelemetrySdk sdk;
  private static Tracer tracer;

  @BeforeAll
  static void init() {
    sdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        new SpanProcessor() {
                          @Override
                          public void onStart(Context parentContext, ReadWriteSpan span) {
                            LocalRootSpan.onSpanStart(span, parentContext);
                          }

                          @Override
                          public boolean isStartRequired() {
                            return true;
                          }

                          @Override
                          public void onEnd(ReadableSpan span) {}

                          @Override
                          public boolean isEndRequired() {
                            return false;
                          }
                        })
                    .build())
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();
    tracer = sdk.getTracer("test-tracer");
  }

  @AfterAll
  static void destroy() {
    sdk.close();
  }

  @Test
  public void checkRemoteParent() {
    Map<String, String> headers = new HashMap<>();
    headers.put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-00f067aa0ba902b7-01");
    Context remoteParent =
        sdk.getPropagators()
            .getTextMapPropagator()
            .extract(Context.root(), headers, new MapGetter());

    ReadWriteSpan span =
        (ReadWriteSpan) tracer.spanBuilder("remote-parent").setParent(remoteParent).startSpan();

    Assertions.assertThat(span.toSpanData().getParentSpanContext().isRemote()).isTrue();
    assertThat(LocalRootSpan.getFor((ReadableSpan) span)).isSameAs(span);
  }

  @Test
  public void checkInferredSpanDetected() {
    Map<String, String> headers = new HashMap<>();
    headers.put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-00f067aa0ba902b7-01");
    Context remoteParent =
        sdk.getPropagators()
            .getTextMapPropagator()
            .extract(Context.root(), headers, new MapGetter());

    ReadWriteSpan delayedInferred =
        (ReadWriteSpan)
            tracer
                .spanBuilder("inferred span with remote parent")
                .setParent(remoteParent)
                .setAttribute(ElasticAttributes.IS_INFERRED, true)
                .startSpan();

    Assertions.assertThat(delayedInferred.toSpanData().getParentSpanContext().isRemote()).isTrue();
    assertThat(LocalRootSpan.getFor((ReadableSpan) delayedInferred)).isNull();

    Span root = tracer.spanBuilder("span1").startSpan();
    Span syncInferred =
        tracer
            .spanBuilder("inferred span with known parent")
            .setParent(Context.root().with(root))
            .setAttribute(ElasticAttributes.IS_INFERRED, true)
            .startSpan();

    assertThat(LocalRootSpan.getFor(syncInferred)).isSameAs(root);
  }

  @Test
  public void checkNested() {
    Span sp1 = tracer.spanBuilder("span1").startSpan();
    Span sp2;
    assertThat(LocalRootSpan.getFor(sp1)).isSameAs(sp1);
    try (Scope s1 = sp1.makeCurrent()) {

      sp2 = tracer.spanBuilder("span2").startSpan();
      assertThat(LocalRootSpan.getFor(sp2)).isSameAs(sp1);
    }

    Span sp3 = tracer.spanBuilder("span3").startSpan();
    assertThat(LocalRootSpan.getFor(sp3)).isSameAs(sp3);
    try (Scope s1 = sp1.makeCurrent()) {
      Span sp4 = tracer.spanBuilder("span4").setParent(Context.root().with(sp2)).startSpan();
      assertThat(LocalRootSpan.getFor(sp4)).isSameAs(sp1);
    }
  }
}
