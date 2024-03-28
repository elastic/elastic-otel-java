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
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.elastic.otel.common.ElasticAttributes;
import co.elastic.otel.testing.DisabledOnAppleSilicon;
import co.elastic.otel.testing.DisabledOnOpenJ9;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

@DisabledOnOs(OS.WINDOWS)
@DisabledOnAppleSilicon
@DisabledOnOpenJ9
public class InferredSpansTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  public void checkInferredSpansFunctional() {
    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> {
              rootSpan();
              try {
                await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(
                        () -> {
                          List<SpanData> spans = testing.spans();
                          assertThat(spans).hasSize(2);

                          assertThat(spans)
                              .anySatisfy(
                                  span -> assertThat(span).hasName("InferredSpansTest.rootSpan"));

                          SpanData parent =
                              spans.stream()
                                  .filter(
                                      span -> span.getName().equals("InferredSpansTest.rootSpan"))
                                  .findFirst()
                                  .get();

                          assertThat(spans)
                              .anySatisfy(
                                  span ->
                                      assertThat(span)
                                          .hasName("InferredSpansTest#rootSpan")
                                          .hasParent(parent)
                                          .hasAttribute(ElasticAttributes.IS_INFERRED, true));
                        });
              } finally {
                testing.clearData();
              }
            });
  }

  @WithSpan
  public void rootSpan() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
