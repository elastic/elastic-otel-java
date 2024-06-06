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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractSimpleChainingSpanProcessorTest {

  private InMemorySpanExporter spans;
  private SpanProcessor exportProcessor;

  @BeforeEach
  public void setup() {
    spans = InMemorySpanExporter.create();
    exportProcessor = SimpleSpanProcessor.create(spans);
  }

  @Test
  public void testSpanDropping() {
    SpanProcessor processor =
        new AbstractSimpleChainingSpanProcessor(exportProcessor) {

          @Override
          protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
            if (readableSpan.getName().startsWith("dropMe")) {
              return null;
            } else {
              return readableSpan;
            }
          }

          @Override
          protected boolean requiresStart() {
            return false;
          }

          @Override
          protected boolean requiresEnd() {
            return true;
          }
        };
    try (OpenTelemetrySdk sdk = sdkWith(processor)) {
      Tracer tracer = sdk.getTracer("dummy-tracer");

      tracer.spanBuilder("dropMe1").startSpan().end();
      tracer.spanBuilder("sendMe").startSpan().end();
      tracer.spanBuilder("dropMe2").startSpan().end();

      assertThat(spans.getFinishedSpanItems())
          .hasSize(1)
          .anySatisfy(span -> assertThat(span).hasName("sendMe"));
    }
  }

  @Test
  public void testAttributeUpdate() {

    AttributeKey<String> keepMeKey = AttributeKey.stringKey("keepMe");
    AttributeKey<String> updateMeKey = AttributeKey.stringKey("updateMe");
    AttributeKey<String> addMeKey = AttributeKey.stringKey("addMe");
    AttributeKey<String> removeMeKey = AttributeKey.stringKey("removeMe");

    SpanProcessor second =
        new AbstractSimpleChainingSpanProcessor(exportProcessor) {
          @Override
          protected boolean requiresStart() {
            return false;
          }

          @Override
          protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
            MutableSpan span = MutableSpan.makeMutable(readableSpan);
            span.setAttribute(addMeKey, "added");
            return span;
          }

          @Override
          protected boolean requiresEnd() {
            return true;
          }
        };
    SpanProcessor first =
        new AbstractSimpleChainingSpanProcessor(second) {
          @Override
          protected boolean requiresStart() {
            return false;
          }

          @Override
          protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
            MutableSpan span = MutableSpan.makeMutable(readableSpan);
            span.setAttribute(updateMeKey, "updated");
            span.removeAttribute(removeMeKey);
            return span;
          }

          @Override
          protected boolean requiresEnd() {
            return true;
          }
        };
    try (OpenTelemetrySdk sdk = sdkWith(first)) {
      Tracer tracer = sdk.getTracer("dummy-tracer");

      tracer
          .spanBuilder("dropMe1")
          .startSpan()
          .setAttribute(keepMeKey, "keep-me-original")
          .setAttribute(removeMeKey, "remove-me-original")
          .setAttribute(updateMeKey, "foo")
          .end();

      assertThat(spans.getFinishedSpanItems())
          .hasSize(1)
          .anySatisfy(
              span -> {
                Attributes attribs = span.getAttributes();
                assertThat(attribs)
                    .hasSize(3)
                    .containsEntry(keepMeKey, "keep-me-original")
                    .containsEntry(updateMeKey, "updated")
                    .containsEntry(addMeKey, "added");
              });
    }
  }

  private OpenTelemetrySdk sdkWith(SpanProcessor processor) {
    return OpenTelemetrySdk.builder()
        .setTracerProvider(SdkTracerProvider.builder().addSpanProcessor(processor).build())
        .build();
  }
}
