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

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.Span;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SpringBootSmokeTest extends SmokeTest {

  private static final String IMAGE =
      "docker.elastic.co/open-telemetry/elastic-otel-java/smoke-test/test-app:latest";

  private static GenericContainer<?> target;

  private static String getUrl(String path) {
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException("path must start with '/'");
    }
    return String.format("http://localhost:%d%s", target.getMappedPort(8080), path);
  }

  @BeforeAll
  public static void start() {
    target = startTarget(IMAGE);
  }

  @AfterAll
  public static void end() {
    if (target != null) {
      target.stop();
    }
  }

  @Test
  public void urlBaseCheck() throws IOException, InterruptedException {
    doRequest(getUrl("/health"), r -> {
      assertThat(r.code()).isEqualTo(200);
      assertThat(r.body()).isNotNull();
      assertThat(r.body().string()).isEqualTo("Alive!");
    });

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).collect(Collectors.toList());
    assertThat(spans)
            .extracting("name", "kind")
            .containsOnly(
                    tuple("GET /health", Span.SpanKind.SPAN_KIND_SERVER),
                    tuple("HealthController.bornToBe", Span.SpanKind.SPAN_KIND_INTERNAL)
            )
            .hasSize(2);


//    spans.stream().filter(s->s.getName().e




    //    Assertions.assertNotNull(response.header("X-server-id"));
    //    Assertions.assertEquals(1, response.headers("X-server-id").size());
    //    Assertions.assertTrue(TraceId.isValid(response.header("X-server-id")));
    //    Assertions.assertEquals("Hi!", response.body().string());
    //    Assertions.assertEquals(1, countSpansByName(traces, "GET /greeting"));
    //    Assertions.assertEquals(0, countSpansByName(traces, "WebController.greeting"));
    //    Assertions.assertEquals(1, countSpansByName(traces, "WebController.withSpan"));
    //    Assertions.assertEquals(2, countSpansByAttributeValue(traces, "custom", "demo"));
    //    Assertions.assertNotEquals(
    //        0, countResourcesByValue(traces, "telemetry.auto.version", currentAgentVersion));
    //    Assertions.assertNotEquals(0, countResourcesByValue(traces, "custom.resource", "demo"));

  }

  public Stream<Span> getSpans(List<ExportTraceServiceRequest> traces) {
    return traces.stream()
            .flatMap(it -> it.getResourceSpansList().stream())
            .flatMap(it -> it.getScopeSpansList().stream())
            .flatMap(it -> it.getSpansList().stream());
  }

  private void doRequest(String url, IOConsumer<Response> responseHandler) throws IOException{
    Request request = new Request.Builder().url(url).get().build();

    try (Response response = client.newCall(request).execute()) {
      responseHandler.accept(response);
    }
  }

  @FunctionalInterface
  private interface IOConsumer<T> {
    void accept(T t) throws IOException;
  }

}
