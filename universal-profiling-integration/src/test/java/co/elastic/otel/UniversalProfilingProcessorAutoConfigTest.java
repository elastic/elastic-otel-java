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

import static co.elastic.otel.UniversalProfilingProcessorAutoConfig.BUFFER_SIZE_OPTION;
import static co.elastic.otel.UniversalProfilingProcessorAutoConfig.ENABLED_OPTION;
import static co.elastic.otel.UniversalProfilingProcessorAutoConfig.SOCKET_DIR_OPTION;
import static co.elastic.otel.UniversalProfilingProcessorAutoConfig.VIRTUAL_THREAD_SUPPORT_OPTION;
import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.otel.testing.AutoConfigTestProperties;
import co.elastic.otel.testing.OtelReflectionUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@DisabledOnOs(OS.WINDOWS)
public class UniversalProfilingProcessorAutoConfigTest {

  @BeforeEach
  @AfterEach
  public void resetGlobalOtel() {
    OtelReflectionUtils.shutdownAndResetGlobalOtel();
  }

  @Test
  public void testDisabling() {
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties().put(ENABLED_OPTION, "false")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).noneMatch(proc -> proc instanceof UniversalProfilingProcessor);
    }
  }

  @Test
  public void checkEnabledButInactiveByDefault() {
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties().put("otel.service.name", "myservice")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors)
          .filteredOn(proc -> proc instanceof UniversalProfilingProcessor)
          .hasSize(1);
      UniversalProfilingProcessor processor =
          (UniversalProfilingProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof UniversalProfilingProcessor)
                  .findFirst()
                  .get();

      assertThat(processor.tlsPropagationActive).isFalse();
      assertThat(processor.tryEnableVirtualThreadSupport).isEqualTo(true);
    }
  }

  @Test
  public void testAllSettings(@TempDir Path tempDir) {

    String tempDirAbs = tempDir.toAbsolutePath().toString();

    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put("otel.service.name", "myservice")
            .put(ENABLED_OPTION, "true")
            .put(BUFFER_SIZE_OPTION, "256")
            .put(SOCKET_DIR_OPTION, tempDirAbs)
            .put(VIRTUAL_THREAD_SUPPORT_OPTION, "false")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      UniversalProfilingProcessor processor =
          (UniversalProfilingProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof UniversalProfilingProcessor)
                  .findFirst()
                  .get();

      assertThat(processor.tlsPropagationActive).isTrue();
      assertThat(processor.socketPath).startsWith(tempDirAbs);
      assertThat(processor.tryEnableVirtualThreadSupport).isEqualTo(false);
      assertThat(processor.correlator.delayedSpans.getBufferSize()).isEqualTo(256);
    }
  }

  @Test
  public void testFailureDoesNotCrashAutoConfig() {
    String badSockerPath = "";
    for (int i = 0; i < 1000; i++) {
      badSockerPath += "abc";
    }

    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put("otel.service.name", "myservice")
            .put(ENABLED_OPTION, "true")
            .put(SOCKET_DIR_OPTION, badSockerPath)) {

      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).isNotEmpty();
      assertThat(processors)
          .filteredOn(proc -> proc instanceof UniversalProfilingProcessor)
          .hasSize(0);
    }
  }
}
