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

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DynamicInstrumentationSmokeTest extends TestAppSmokeTest {

  @BeforeAll
  public static void start() {
    startTestApp(
        (container) -> {
          container.addEnv(
              "OTEL_INSTRUMENTATION_METHODS_INCLUDE",
              "co.elastic.otel.test.DynamicInstrumentationController[flipMethods,flipAll]");
          container.addEnv(
              "ELASTIC_OTEL_JAVA_EXPERIMENTAL_DISABLE_INSTRUMENTATIONS_CHECKER", "true");
          container.addEnv(
              "ELASTIC_OTEL_JAVA_EXPERIMENTAL_DISABLE_INSTRUMENTATIONS_CHECKER_INTERVAL_MS", "300");
          container.addEnv("OTEL_JAVAAGENT_DEBUG", "true");
        });
  }

  @AfterAll
  public static void end() {
    stopApp();
  }

  @AfterEach
  public void endTest() throws InterruptedException {
    doRequest(getUrl("/dynamic/reset"), okResponseBody("reset"));
    Thread.sleep(500L); // give the reset time to be applied
  }

  @Test
  public void flipMethodInstrumentation() throws InterruptedException {
    dynamicFlipInstrumentation("Methods", 1);
  }

  @Test
  public void flipAllInstrumentation() throws InterruptedException {
    dynamicFlipInstrumentation("All", 0);
  }

  private void dynamicFlipInstrumentation(String extensionName, int SpanCountWhenDisabled)
      throws InterruptedException {
    doRequest(getUrl("/dynamic/flip" + extensionName), okResponseBody("enabled"));
    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans)
        .hasSize(2)
        .extracting("name")
        .containsOnly(
            "GET /dynamic/flip" + extensionName,
            "DynamicInstrumentationController.flip" + extensionName);
    ByteString firstTraceID = spans.get(0).getTraceId();

    Thread.sleep(500L); // give the flip time to be applied

    doRequest(getUrl("/dynamic/flip" + extensionName), okResponseBody("disabled"));
    traces = waitForTraces();
    spans = getSpans(traces).dropWhile(span -> span.getTraceId().equals(firstTraceID)).toList();
    if (SpanCountWhenDisabled > 0) {
      assertThat(spans)
          .hasSize(SpanCountWhenDisabled)
          .extracting("name")
          .containsOnly("GET /dynamic/flip" + extensionName);
    } else {
      assertThat(spans).hasSize(0);
    }
    ByteString secondTraceID = SpanCountWhenDisabled > 0 ? spans.get(0).getTraceId() : firstTraceID;

    Thread.sleep(500L); // give the flip time to be applied

    doRequest(getUrl("/dynamic/flip" + extensionName), okResponseBody("enabled"));
    traces = waitForTraces();
    spans =
        getSpans(traces)
            .dropWhile(
                span ->
                    span.getTraceId().equals(firstTraceID)
                        || span.getTraceId().equals(secondTraceID))
            .toList();
    assertThat(spans)
        .hasSize(2)
        .extracting("name")
        .containsOnly(
            "GET /dynamic/flip" + extensionName,
            "DynamicInstrumentationController.flip" + extensionName);
  }
}
