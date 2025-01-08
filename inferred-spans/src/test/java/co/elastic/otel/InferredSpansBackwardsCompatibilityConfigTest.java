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

import co.elastic.otel.testing.AutoConfigTestProperties;
import co.elastic.otel.testing.DisabledOnOpenJ9;
import co.elastic.otel.testing.OtelReflectionUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.contrib.inferredspans.FieldAccessors;
import io.opentelemetry.contrib.inferredspans.InferredSpansProcessor;
import io.opentelemetry.contrib.inferredspans.WildcardMatcher;
import io.opentelemetry.contrib.inferredspans.internal.InferredSpansConfiguration;
import io.opentelemetry.contrib.inferredspans.internal.ProfilingActivationListener;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class InferredSpansBackwardsCompatibilityConfigTest {

  @BeforeEach
  @AfterEach
  public void resetGlobalOtel() {
    ProfilingActivationListener.ensureInitialized();
    OtelReflectionUtils.shutdownAndResetGlobalOtel();
  }

  @Test
  @DisabledOnOpenJ9
  public void checkAllLegacyOptions(@TempDir Path tmpDir) {
    String libDir = tmpDir.resolve("foo").resolve("bar").toString();
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put("elastic.otel.inferred.spans.enabled", "true")
            .put("elastic.otel.inferred.spans.logging.enabled", "false")
            .put("elastic.otel.inferred.spans.backup.diagnostic.files", "true")
            .put("elastic.otel.inferred.spans.safe.mode", "16")
            .put("elastic.otel.inferred.spans.post.processing.enabled", "false")
            .put("elastic.otel.inferred.spans.sampling.interval", "7ms")
            .put("elastic.otel.inferred.spans.min.duration", "2ms")
            .put("elastic.otel.inferred.spans.included.classes", "foo*23,bar.baz")
            .put("elastic.otel.inferred.spans.excluded.classes", "blub,test*.test2")
            .put("elastic.otel.inferred.spans.interval", "2s")
            .put("elastic.otel.inferred.spans.duration", "3s")
            .put("elastic.otel.inferred.spans.lib.directory", libDir)) {

      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).filteredOn(proc -> proc instanceof InferredSpansProcessor).hasSize(1);
      InferredSpansProcessor processor =
          (InferredSpansProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof InferredSpansProcessor)
                  .findFirst()
                  .get();

      InferredSpansConfiguration config = FieldAccessors.getProfiler(processor).getConfig();
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
      assertThat(config.getProfilerLibDirectory()).isEqualTo(libDir);
    }
  }


  @Test
  @DisabledOnOpenJ9
  public void ensureOptionsTakePrecedenceOverLegacyOptions() {
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put("elastic.otel.inferred.spans.enabled", "false")
            .put("otel.inferred.spans.enabled", "true")
            .put("elastic.otel.inferred.spans.interval", "2s")
            .put("otel.inferred.spans.interval", "3s")
    ) {

      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).filteredOn(proc -> proc instanceof InferredSpansProcessor).hasSize(1);
      InferredSpansProcessor processor =
          (InferredSpansProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof InferredSpansProcessor)
                  .findFirst()
                  .get();

      InferredSpansConfiguration config = FieldAccessors.getProfiler(processor).getConfig();
      assertThat(config.getProfilingInterval()).isEqualTo(Duration.ofSeconds(3));
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

  private List<String> wildcardsAsStrings(List<WildcardMatcher> wildcardList) {
    return wildcardList.stream().map(WildcardMatcher::getMatcher).collect(Collectors.toList());
  }
}
