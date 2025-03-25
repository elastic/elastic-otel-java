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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeAttachSmokeTest extends TestAppSmokeTest {

  @BeforeAll
  public static void start() {
    startTestApp(
        container -> {
          String jvmOptions = container.getEnvMap().get("JAVA_TOOL_OPTIONS");
          if (jvmOptions != null) {
            // remove '-javaagent' from JVM args
            jvmOptions =
                Arrays.asList(jvmOptions.split(" ")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.startsWith("-javaagent:"))
                    .collect(Collectors.joining());
            container.withEnv("JAVA_TOOL_OPTIONS", jvmOptions);
          }

          // make the app use runtime-attach
          container.withEnv("TEST_RUNTIME_ATTACH", "true");
        });
  }

  @AfterAll
  public static void end() {
    stopApp();
  }

  @Test
  void runtimeAttachTrace() {
    // runtime attach is working if we can capture any signal, for simplicity we only test traces

    doRequest(getUrl("/health"), okResponseBody("Alive!"));

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans).hasSize(1).extracting("name").containsOnly("GET /health");
  }

}
