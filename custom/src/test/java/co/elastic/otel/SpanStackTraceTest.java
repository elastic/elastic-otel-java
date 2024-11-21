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
import co.elastic.otel.testing.AutoConfigTestProperties;
import co.elastic.otel.testing.AutoConfiguredDataCapture;
import co.elastic.otel.testing.OtelReflectionUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import java.util.List;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests Span stack trace capture feature is properly behaving, which is mostly ensuring that the
 * contrib implementation is properly configured for usage in this distribution.
 */
public class SpanStackTraceTest {

  private static final String OTEL_MIN_DURATION =
      "otel.java.experimental.span-stacktrace.min.duration";

  @BeforeEach
  @AfterEach
  void resetGlobalOtel() {
    OtelReflectionUtils.shutdownAndResetGlobalOtel();
  }

  @Test
  void checkStackTracePresent() {
    try (AutoConfigTestProperties testProps =
        new AutoConfigTestProperties().put(OTEL_MIN_DURATION, "0ms")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();

      Tracer tracer = otel.getTracer("test-tracer");

      tracer.spanBuilder("my-span").startSpan().end();

      checkSpanStackTrace(true);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        ElasticAutoConfigurationCustomizerProvider.STACKTRACE_OTEL_DURATION,
        ElasticAutoConfigurationCustomizerProvider.STACKTRACE_LEGACY1_DURATION,
        ElasticAutoConfigurationCustomizerProvider.STACKTRACE_LEGACY2_DURATION
      })
  void featureCanBeDisabled(String configName) {
    try (AutoConfigTestProperties testProps =
        new AutoConfigTestProperties().put(configName, "-1")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();

      assertThat(OtelReflectionUtils.getSpanProcessors(otel))
          .noneSatisfy(proc -> assertThat(proc).isInstanceOf(StackTraceSpanProcessor.class));
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        ElasticAutoConfigurationCustomizerProvider.STACKTRACE_OTEL_DURATION,
        ElasticAutoConfigurationCustomizerProvider.STACKTRACE_LEGACY1_DURATION,
        ElasticAutoConfigurationCustomizerProvider.STACKTRACE_LEGACY2_DURATION
      })
  void legacyConfigOptionsSupported(String configName) {
    try (AutoConfigTestProperties testProps =
        new AutoConfigTestProperties().put(configName, "0ms")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();

      Tracer tracer = otel.getTracer("test-tracer");

      tracer.spanBuilder("my-span").startSpan().end();

      checkSpanStackTrace(true);
    }
  }

  @Test
  void checkMinDurationRespected() {
    try (AutoConfigTestProperties testProps =
        new AutoConfigTestProperties().put(OTEL_MIN_DURATION, "100s")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();

      Tracer tracer = otel.getTracer("test-tracer");

      tracer.spanBuilder("my-span").startSpan().end();

      checkSpanStackTrace(false);
    }
  }

  private void checkSpanStackTrace(boolean stackTraceExpected) {
    List<SpanData> spans = AutoConfiguredDataCapture.getSpans();
    assertThat(spans).hasSize(1);
    SpanData span = spans.get(0);
    if (stackTraceExpected) {
      assertThat(span)
          .hasAttribute(
              satisfies(
                  CodeIncubatingAttributes.CODE_STACKTRACE,
                  AbstractCharSequenceAssert::isNotBlank));
    } else {
      assertThat(span.getAttributes().get(CodeIncubatingAttributes.CODE_STACKTRACE)).isNull();
    }
  }

  @Test
  void checkInferredSpansIgnored() {
    try (AutoConfigTestProperties testProps =
        new AutoConfigTestProperties().put(OTEL_MIN_DURATION, "0")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();

      Tracer tracer = otel.getTracer("test-tracer");

      tracer
          .spanBuilder("my-span")
          .setAttribute(ElasticAttributes.IS_INFERRED, true)
          .startSpan()
          .end();

      checkSpanStackTrace(false);
    }
  }
}
