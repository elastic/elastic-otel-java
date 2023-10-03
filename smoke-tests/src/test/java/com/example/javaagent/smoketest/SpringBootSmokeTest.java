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
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class SpringBootSmokeTest extends SmokeTest {

  private static final String IMAGE =
      "docker.elastic.co/open-telemetry/elastic-otel-java/smoke-test/test-app:latest";

  private static GenericContainer<?> target;

  private static String getBaseUrl() {
    return String.format("http://localhost:%d/", target.getMappedPort(8080));
  }

  @BeforeAll
  public static void start() {
    target = startTarget(IMAGE);
  }

  @AfterAll
  public static void end() {
    target.stop();
  }

  @Test
  public void springBootSmokeTestOnJDK() throws IOException, InterruptedException {
    Request request = new Request.Builder().url(getBaseUrl()).get().build();

    try (Response response = client.newCall(request).execute()) {
      Assertions.assertEquals(response.body().string(), "hello");
    }

    Collection<ExportTraceServiceRequest> traces = waitForTraces();
    Assertions.assertEquals(1, traces.size());


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

}
