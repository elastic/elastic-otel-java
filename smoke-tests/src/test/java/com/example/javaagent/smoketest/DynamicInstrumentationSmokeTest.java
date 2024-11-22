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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DynamicInstrumentationSmokeTest extends TestAppSmokeTest {

  @BeforeAll
  public static void start() {
    startTestApp(
        (container) -> {
          container.addEnv(
              "OTEL_INSTRUMENTATION_METHODS_INCLUDE",
              "co.elastic.otel.test.DynamicInstrumentationController[flipMethods]");
          container.addEnv("ELASTIC_OTEL_JAVA_DISABLE_INSTRUMENTATIONS_CHECKER", "true");
          container.addEnv("OTEL_JAVAAGENT_DEBUG", "true");
        });
  }

  @AfterAll
  public static void end() {
    stopApp();
  }

  @Test
  public void flipMethodInstrumentation() throws InterruptedException {
    doRequest(getUrl("/dynamic"), okResponseBody("enabled"));
    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans)
        .hasSize(2)
        .extracting("name")
        .containsOnly("GET /dynamic", "DynamicInstrumentationController.flipMethods");
    ByteString firstTraceID = spans.get(0).getTraceId();

    Thread.sleep(2000L); // give the flip time to be applied

    doRequest(getUrl("/dynamic"), okResponseBody("disabled"));
    traces = waitForTraces();
    spans = getSpans(traces).dropWhile(span -> span.getTraceId().equals(firstTraceID)).toList();
    assertThat(spans).hasSize(1).extracting("name").containsOnly("GET /dynamic");
    ByteString secondTraceID = spans.get(0).getTraceId();

    Thread.sleep(2000L); // give the flip time to be applied

    doRequest(getUrl("/dynamic"), okResponseBody("enabled"));
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
        .containsOnly("GET /dynamic", "DynamicInstrumentationController.flipMethods");
  }
}
