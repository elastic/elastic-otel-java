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
package com.example.javaagent.smoketest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InferredSpansSmokeTest extends TestAppSmokeTest {

  @BeforeAll
  public static void start() {
    startTestApp(
        container -> {
          String jvmOptions = container.getEnvMap().get("JAVA_TOOL_OPTIONS");
          if (jvmOptions == null) {
            jvmOptions = "";
          }
          jvmOptions += " -Delastic.otel.inferred.spans.enabled=true";
          jvmOptions += " -Delastic.otel.inferred.spans.duration=500ms";
          jvmOptions += " -Delastic.otel.inferred.spans.interval=500ms";
          jvmOptions += " -Delastic.otel.inferred.spans.sampling.interval=5ms";
          container.withEnv("JAVA_TOOL_OPTIONS", jvmOptions);
        });
  }

  @AfterAll
  public static void after() {
    stopApp();
  }

  @Test
  void checkInferredSpansFunctional() {

    doRequest(getUrl("/inferred-spans/sleep?millis=50"), okResponse());

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              List<ExportTraceServiceRequest> traces = waitForTraces();
              List<Span> spans = getSpans(traces).toList();

              String PARENT_SPAN_NAME = "InferredSpansController.doSleep";
              // The name of the inferred span may vary based on timing, therefore we simply look
              // for a span containing "#" in the name

              assertThat(spans)
                  .anySatisfy(span -> assertThat(span.getName()).isEqualTo(PARENT_SPAN_NAME));

              Span parent =
                  spans.stream()
                      .filter(span -> span.getName().equals(PARENT_SPAN_NAME))
                      .findFirst()
                      .get();

              assertThat(spans)
                  .anySatisfy(
                      child -> {
                        assertThat(child.getName()).contains("#");
                        assertThat(child.getTraceId()).isEqualTo(parent.getTraceId());
                        assertThat(child.getAttributesList())
                            .anySatisfy(
                                attrib -> {
                                  assertThat(attrib.getKey()).isEqualTo("elastic.is_inferred");
                                  assertThat(attrib.getValue().getBoolValue()).isEqualTo(true);
                                });
                        assertThat(child.getEndTimeUnixNano() - child.getStartTimeUnixNano())
                            .isGreaterThan(30_000_000L);
                      });
            });
  }
}
