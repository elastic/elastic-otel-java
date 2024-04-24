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

import co.elastic.otel.testing.AutoConfigTestProperties;
import co.elastic.otel.testing.AutoConfiguredDataCapture;
import co.elastic.otel.testing.OtelReflectionUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpanStackTraceProcessorAutoConfigTest {
  @BeforeEach
  @AfterEach
  void resetGlobalOtel() {
    OtelReflectionUtils.shutdownAndResetGlobalOtel();
  }

  @Test
  void checkStackTracePresent() {
    try (AutoConfigTestProperties testProps =
        new AutoConfigTestProperties()
            .put(SpanStackTraceProcessorAutoConfig.MIN_DURATION_CONFIG_OPTION, "0ms")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();

      // TODO: cleanup other extensions (Breakdown) so that this is not required:
      ElasticExtension.INSTANCE.registerOpenTelemetry(otel);

      Tracer tracer = otel.getTracer("test-tracer");

      tracer.spanBuilder("my-span").startSpan().end();

      List<SpanData> spans = AutoConfiguredDataCapture.getSpans();

      assertThat(spans).hasSize(1);
      assertThat(spans.get(0))
          .hasName("my-span")
          .hasAttribute(
              satisfies(CodeIncubatingAttributes.CODE_STACKTRACE, att -> att.isNotBlank()));
    }
  }

  @Test
  void checkMinDurationRespected() {
    try (AutoConfigTestProperties testProps =
        new AutoConfigTestProperties()
            .put(SpanStackTraceProcessorAutoConfig.MIN_DURATION_CONFIG_OPTION, "100s")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();

      // TODO: cleanup other extensions (Breakdown) so that this is not required:
      ElasticExtension.INSTANCE.registerOpenTelemetry(otel);

      Tracer tracer = otel.getTracer("test-tracer");

      tracer.spanBuilder("my-span").startSpan().end();

      List<SpanData> spans = AutoConfiguredDataCapture.getSpans();
      assertThat(spans).hasSize(0);
    }
  }
}
