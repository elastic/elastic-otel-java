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
public class InferredSpansTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  public void checkInferredSpansFunctional() {
    // Presumably due to the CI delay it looks like sometimes inferred spans is not able
    // to generate spans for the first normal span
    // To avoid flakyness we therefore do multiple attempts
    await()
        .atMost(Duration.ofSeconds(30))
        .ignoreExceptions()
        .until(
            () -> {
              rootSpan();
              try {
                await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(
                        () -> {
                          List<SpanData> spans = testing.spans();
                          assertThat(spans).hasSizeGreaterThanOrEqualTo(3);

                          assertThat(spans)
                              .anySatisfy(
                                  span -> assertThat(span).hasName("InferredSpansTest.rootSpan"))
                              .anySatisfy(
                                  span -> assertThat(span).hasName("InferredSpansTest.childSpan"));

                          SpanData parent =
                              spans.stream()
                                  .filter(
                                      span -> span.getName().equals("InferredSpansTest.rootSpan"))
                                  .findFirst()
                                  .get();

                          SpanData child =
                              spans.stream()
                                  .filter(
                                      span -> span.getName().equals("InferredSpansTest.childSpan"))
                                  .findFirst()
                                  .get();

                          assertThat(spans)
                              .anySatisfy(
                                  span ->
                                      assertThat(span)
                                          .hasName("InferredSpansTest#rootSpan")
                                          .hasParent(parent)
                                          .hasAttribute(ElasticAttributes.IS_INFERRED, true)
                                          .hasLinksSatisfying(
                                              links ->
                                                  assertThat(links)
                                                      .hasSize(1)
                                                      .anySatisfy(
                                                          link -> {
                                                            assertThat(
                                                                link.getSpanContext()
                                                                    .getSpanId())
                                                                .isEqualTo(child.getSpanId());
                                                            assertThat(link.getAttributes())
                                                                .containsEntry("is_child", true)
                                                                .containsEntry(
                                                                    "elastic.is_child", true);
                                                          })));
                        });
                return true;
              } finally {
                testing.clearData();
              }
            });
  }

  @WithSpan
  public void rootSpan() {
    try {
      Thread.sleep(100);
      childSpan();
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @WithSpan
  public void childSpan() {}
}
