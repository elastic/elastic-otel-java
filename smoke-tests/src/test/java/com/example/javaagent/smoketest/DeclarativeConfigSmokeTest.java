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
import static org.assertj.core.api.Assertions.tuple;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

public class DeclarativeConfigSmokeTest extends TestAppSmokeTest {

  private static void extractDeclarativeConfig(Path targetPath) {
    try {
      Process process =
          new ProcessBuilder()
              .command(JavaExecutable.getBinaryPath(), "-jar", AGENT_PATH, "--default-config-yaml")
              .redirectOutput(targetPath.toFile())
              .start();
      boolean processExit = process.waitFor(5, TimeUnit.SECONDS);
      if (!processExit || process.exitValue() != 0) {
        throw new IllegalStateException("failed to get default declarative config");
      }
    } catch (InterruptedException | IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @BeforeAll
  static void start() throws IOException {

    Path configFile = Files.createTempFile("tmp", ".yaml");
    extractDeclarativeConfig(configFile);

    // need to do some post-processing to switch from http to grpc by default
    List<String> modifiedConfig =
        Files.readAllLines(configFile).stream()
            .map(
                line -> {
                  if (line.contains("otlp_http")) {
                    // replace http exporter to grpc
                    return line.replace("otlp_http", "otlp_grpc");
                  } else if (line.contains("endpoint:")) {
                    // for grpc the endpoint should not have a path like with HTTP
                    int index = line.indexOf("endpoint:");
                    return line.substring(0, index + 10) + "${OTEL_EXPORTER_OTLP_ENDPOINT}";
                  } else {
                    return line;
                  }
                })
            .collect(Collectors.toList());
    Files.write(configFile, modifiedConfig);

    startTestApp(
        (container) ->
            container
                .withCopyFileToContainer(MountableFile.forHostPath(configFile), "/config.yaml")
                .withEnv("OTEL_CONFIG_FILE", "/config.yaml")
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", BACKEND_ENDPOINT),
        false);
  }

  @Test
  void healthcheck() {
    doRequest(getUrl("/health"), okResponseBody("Alive!"));

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans)
        .hasSize(1)
        .extracting("name", "kind")
        .containsOnly(tuple("GET /health", Span.SpanKind.SPAN_KIND_SERVER));
  }

  @AfterAll
  static void end() {
    stopApp();
  }
}
