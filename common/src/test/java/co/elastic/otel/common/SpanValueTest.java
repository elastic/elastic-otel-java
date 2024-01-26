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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

public class SpanValueTest {

  private static OpenTelemetrySdk sdk;

  private static Tracer tracer;

  private static Span earlySpan;

  @BeforeAll
  static void initSdk() {
    sdk =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    // We need to register an exporter to force the SDK to be non-NoOp
                    .addSpanProcessor(SimpleSpanProcessor.create(InMemorySpanExporter.create()))
                    .build())
            .build();
    tracer = sdk.getTracer("test-tracer");

    // This span was created and has it's SpanValue storage initialized before
    // other dense SpanValue in tests are created
    // as a result, the backing AtomicReferenceArray is not used for those and they should fall back
    // to the map-based storage
    earlySpan = tracer.spanBuilder("early span").startSpan();
    SpanValue.createSparse().set(earlySpan, "foo");
  }

  @AfterAll
  static void shutdownSdk() {
    sdk.close();
  }

  static Span newSpan() {
    Span span = tracer.spanBuilder("new span").startSpan();
    span.end();
    return span;
  }

  public static Stream<Arguments> testArgs() {
    return Stream.of(
        Arguments.of(
            Named.of(
                "Early Span, dense SpanValue",
                ValueAccess.create(SpanValue.<String>createDense(), earlySpan))),
        Arguments.of(
            Named.of(
                "Early Span, sparse SpanValue",
                ValueAccess.create(SpanValue.<String>createSparse(), earlySpan))),
        Arguments.of(
            Named.of(
                "Early ReadableSpan, dense SpanValue",
                ValueAccess.create(SpanValue.<String>createDense(), (ReadableSpan) earlySpan))),
        Arguments.of(
            Named.of(
                "Early ReadableSpan, sparse SpanValue",
                ValueAccess.create(SpanValue.<String>createSparse(), (ReadableSpan) earlySpan))),
        Arguments.of(
            Named.of(
                "New Span, dense SpanValue",
                ValueAccess.create(SpanValue.<String>createDense(), newSpan()))),
        Arguments.of(
            Named.of(
                "New Span, sparse SpanValue",
                ValueAccess.create(SpanValue.<String>createSparse(), newSpan()))),
        Arguments.of(
            Named.of(
                "MutableSpan, dense SpanValue",
                ValueAccess.create(
                    SpanValue.<String>createDense(),
                    MutableSpan.makeMutable((ReadableSpan) newSpan())))),
        Arguments.of(
            Named.of(
                "MutableSpan, sparse SpanValue",
                ValueAccess.create(
                    SpanValue.<String>createSparse(),
                    MutableSpan.makeMutable((ReadableSpan) newSpan())))));
  }

  @ParameterizedTest
  @MethodSource("testArgs")
  public void checkSet(ValueAccess<String> accessor) {
    assertThat(accessor.get()).isNull();
    accessor.set("initial");
    assertThat(accessor.get()).isEqualTo("initial");
    accessor.set("override");
    assertThat(accessor.get()).isEqualTo("override");
    accessor.set(null);
    assertThat(accessor.get()).isEqualTo(null);
  }

  @ParameterizedTest
  @MethodSource("testArgs")
  public void checkSetIfNullAndClear(ValueAccess<String> accessor) {
    assertThat(accessor.get()).isNull();
    assertThat(accessor.setIfNull(null)).isTrue();
    assertThat(accessor.setIfNull("initial")).isTrue();
    assertThat(accessor.get()).isEqualTo("initial");
    assertThat(accessor.setIfNull("override")).isFalse();
    assertThat(accessor.setIfNull(null)).isFalse();
    assertThat(accessor.get()).isEqualTo("initial");
    accessor.clear();
    assertThat(accessor.get()).isEqualTo(null);
    assertThat(accessor.setIfNull("override")).isTrue();
    assertThat(accessor.get()).isEqualTo("override");
  }

  @ParameterizedTest
  @MethodSource("testArgs")
  @SuppressWarnings("unchecked")
  public void checkComputeIfNull(ValueAccess<String> accessor) {
    Supplier<String> initializer = (Supplier<String>) Mockito.mock(Supplier.class);
    doReturn(null).when(initializer).get();

    assertThat(accessor.computeIfNull(initializer)).isNull();
    verify(initializer).get();
    assertThat(accessor.get()).isNull();

    doReturn("init1").when(initializer).get();
    assertThat(accessor.computeIfNull(initializer)).isEqualTo("init1");
    verify(initializer, times(2)).get();
    assertThat(accessor.get()).isEqualTo("init1");

    doReturn("init2").when(initializer).get();
    assertThat(accessor.computeIfNull(initializer)).isEqualTo("init1");
    verify(initializer, times(2)).get();
    assertThat(accessor.get()).isEqualTo("init1");
  }

  @Test
  public void testInvalidSpanDetected() {
    assertThatThrownBy(() -> SpanValue.createSparse().get(Span.getInvalid()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid span");
  }

  @Test
  public void verifyMutableSpanUnwrapped() {
    SpanValue<String> val = SpanValue.createSparse();
    ReadableSpan span = (ReadableSpan) newSpan();
    MutableSpan wrappedOnce = MutableSpan.makeMutable(span);
    wrappedOnce.toSpanData(); // freeze MutableSpan to double-wrap
    MutableSpan wrappedTwice = MutableSpan.makeMutable(wrappedOnce);
    assertThat(wrappedOnce).isNotSameAs(wrappedTwice);

    val.set(span, "original");
    assertThat(val.get(span)).isEqualTo("original");
    assertThat(val.get(wrappedOnce)).isEqualTo("original");
    assertThat(val.get(wrappedTwice)).isEqualTo("original");

    val.set(wrappedOnce, "wrapped1");
    assertThat(val.get(span)).isEqualTo("wrapped1");
    assertThat(val.get(wrappedOnce)).isEqualTo("wrapped1");
    assertThat(val.get(wrappedTwice)).isEqualTo("wrapped1");

    val.set(wrappedTwice, "wrapped2");
    assertThat(val.get(span)).isEqualTo("wrapped2");
    assertThat(val.get(wrappedOnce)).isEqualTo("wrapped2");
    assertThat(val.get(wrappedTwice)).isEqualTo("wrapped2");
  }

  /**
   * Utility for writing checks agnostic of whether {@link Span} or {@link
   * io.opentelemetry.sdk.trace.ReadableSpan} is used.
   */
  public interface ValueAccess<T> {

    T get();

    void set(T val);

    boolean setIfNull(T val);

    T computeIfNull(Supplier<T> initializer);

    void clear();

    static <T> ValueAccess<T> create(SpanValue<T> spanVal, Span span) {
      return new ValueAccess<T>() {
        @Override
        public T get() {
          return spanVal.get(span);
        }

        @Override
        public void set(T val) {
          spanVal.set(span, val);
        }

        @Override
        public boolean setIfNull(T val) {
          return spanVal.setIfNull(span, val);
        }

        @Override
        public T computeIfNull(Supplier<T> initializer) {
          return spanVal.computeIfNull(span, initializer);
        }

        @Override
        public void clear() {
          spanVal.clear(span);
        }
      };
    }

    static <T> ValueAccess<T> create(SpanValue<T> spanVal, ReadableSpan span) {
      return new ValueAccess<T>() {
        @Override
        public T get() {
          return spanVal.get(span);
        }

        @Override
        public void set(T val) {
          spanVal.set(span, val);
        }

        @Override
        public boolean setIfNull(T val) {
          return spanVal.setIfNull(span, val);
        }

        @Override
        public T computeIfNull(Supplier<T> initializer) {
          return spanVal.computeIfNull(span, initializer);
        }

        @Override
        public void clear() {
          spanVal.clear(span);
        }
      };
    }
  }
}
