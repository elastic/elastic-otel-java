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
package co.elastic.otel.profiler;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.elastic.otel.profiler.config.WildcardMatcher;
import co.elastic.otel.testing.AutoConfigTestProperties;
import co.elastic.otel.testing.AutoConfiguredDataCapture;
import co.elastic.otel.testing.DisabledOnAppleSilicon;
import co.elastic.otel.testing.OtelReflectionUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class InferredSpansAutoConfigTest {

  @BeforeEach
  @AfterEach
  public void resetGlobalOtel() {
    ProfilingActivationListener.ensureInitialized();
    OtelReflectionUtils.shutdownAndResetGlobalOtel();
  }

  @Test
  public void checkAllOptions() {
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put(InferredSpansAutoConfig.ENABLED_OPTION, "true")
            .put(InferredSpansAutoConfig.LOGGING_OPTION, "false")
            .put(InferredSpansAutoConfig.DIAGNOSTIC_FILES_OPTION, "true")
            .put(InferredSpansAutoConfig.SAFEMODE_OPTION, "16")
            .put(InferredSpansAutoConfig.POSTPROCESSING_OPTION, "false")
            .put(InferredSpansAutoConfig.SAMPLING_INTERVAL_OPTION, "7ms")
            .put(InferredSpansAutoConfig.MIN_DURATION_OPTION, "2ms")
            .put(InferredSpansAutoConfig.INCLUDED_CLASSES_OPTION, "foo*23,bar.baz")
            .put(InferredSpansAutoConfig.EXCLUDED_CLASSES_OPTION, "blub,test*.test2")
            .put(InferredSpansAutoConfig.INTERVAL_OPTION, "2s")
            .put(InferredSpansAutoConfig.DURATION_OPTION, "3s")
            .put(InferredSpansAutoConfig.LIB_DIRECTORY_OPTION, "/tmp/somewhere")) {

      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).filteredOn(proc -> proc instanceof InferredSpansProcessor).hasSize(1);
      InferredSpansProcessor processor =
          (InferredSpansProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof InferredSpansProcessor)
                  .findFirst()
                  .get();

      InferredSpansConfiguration config = processor.profiler.config;
      assertThat(config.isProfilingLoggingEnabled()).isFalse();
      assertThat(config.isBackupDiagnosticFiles()).isTrue();
      assertThat(config.getAsyncProfilerSafeMode()).isEqualTo(16);
      assertThat(config.getSamplingInterval()).isEqualTo(Duration.ofMillis(7));
      assertThat(wildcardsAsStrings(config.getIncludedClasses()))
          .containsExactly("foo*23", "bar.baz");
      assertThat(wildcardsAsStrings(config.getExcludedClasses()))
          .containsExactly("blub", "test*.test2");
      assertThat(config.getProfilingInterval()).isEqualTo(Duration.ofSeconds(2));
      assertThat(config.getProfilingDuration()).isEqualTo(Duration.ofSeconds(3));
      assertThat(config.getProfilerLibDirectory()).isEqualTo("/tmp/somewhere");
    }
  }

  @Test
  public void checkDisabledbyDefault() {
    try (AutoConfigTestProperties props = new AutoConfigTestProperties()) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).noneMatch(proc -> proc instanceof InferredSpansProcessor);
    }
  }

  @DisabledOnAppleSilicon
  @DisabledOnOs(OS.WINDOWS)
  @Test
  public void checkProfilerWorking() {
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put(InferredSpansAutoConfig.ENABLED_OPTION, "true")
            .put(InferredSpansAutoConfig.DURATION_OPTION, "500ms")
            .put(InferredSpansAutoConfig.INTERVAL_OPTION, "500ms")
            .put(InferredSpansAutoConfig.SAMPLING_INTERVAL_OPTION, "5ms")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).filteredOn(proc -> proc instanceof InferredSpansProcessor).hasSize(1);
      InferredSpansProcessor processor =
          (InferredSpansProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof InferredSpansProcessor)
                  .findFirst()
                  .get();

      // Wait until profiler is started
      await()
          .pollDelay(10, TimeUnit.MILLISECONDS)
          .timeout(6000, TimeUnit.MILLISECONDS)
          .until(() -> processor.profiler.getProfilingSessions() > 1);

      Tracer tracer = otel.getTracer("manual-spans");

      Span tx = tracer.spanBuilder("my-root").startSpan();
      try (Scope scope = tx.makeCurrent()) {
        doSleep();
      } finally {
        tx.end();
      }

      await()
          .untilAsserted(
              () ->
                  assertThat(AutoConfiguredDataCapture.getSpans())
                      .hasSizeGreaterThanOrEqualTo(2)
                      .anySatisfy(
                          span -> {
                            assertThat(span.getName()).startsWith("InferredSpansAutoConfigTest#");
                            assertThat(span.getInstrumentationScopeInfo().getName())
                                .isEqualTo(InferredSpansProcessor.TRACER_NAME);
                            assertThat(span.getInstrumentationScopeInfo().getVersion())
                                .isNotBlank();
                          }));
    }
  }

  private void doSleep() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> wildcardsAsStrings(List<WildcardMatcher> wildcardList) {
    return wildcardList.stream().map(WildcardMatcher::getMatcher).collect(Collectors.toList());
  }
}
