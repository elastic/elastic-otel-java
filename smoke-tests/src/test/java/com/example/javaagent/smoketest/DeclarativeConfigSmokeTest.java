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

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_DISTRO_NAME;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_DISTRO_VERSION;
import static io.opentelemetry.semconv.incubating.ContainerIncubatingAttributes.CONTAINER_ID;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_ARCH;
import static io.opentelemetry.semconv.incubating.HostIncubatingAttributes.HOST_NAME;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE;
import static io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION;
import static io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS;
import static io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes.PROCESS_PID;
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
      if (!processExit) {
        process.destroyForcibly();
      }
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
                .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", BACKEND_ENDPOINT)
                .withEnv("OTEL_SERVICE_NAME", "my-service-name"),
        false);
  }

  @Test
  void resourceAttributes() {
    doRequest(getUrl("/health"), okResponseBody("Alive!"));

    // This does not test that cloud resource providers are included in the config
    // but at least we know that if they are added in the config their names are valid as otherwise
    // the agent does not start with any missing component in config.

    List<ExportTraceServiceRequest> traces = waitForTraces();
    List<Span> spans = getSpans(traces).toList();
    assertThat(spans)
        .hasSize(1)
        .extracting("name", "kind")
        .containsOnly(tuple("GET /health", Span.SpanKind.SPAN_KIND_SERVER));

    assertThat(getResourceAttributes(traces))
        .containsKey(HOST_NAME.getKey())
        .containsKey(HOST_ARCH.getKey())
        .containsKey(OS_TYPE.getKey())
        .containsKey(OS_VERSION.getKey())
        .containsKey(PROCESS_PID.getKey())
        .containsKey(PROCESS_COMMAND_ARGS.getKey())
        .containsKey(CONTAINER_ID.getKey())
        // service name should be provided by the 'service' resource attribute detector which
        // reads environment variables (here it's not env variable injected in declarative config).
        .containsEntry(SERVICE_NAME.getKey(), attributeValue("my-service-name"))
        .containsKey(SERVICE_INSTANCE_ID.getKey())
        // ensures that we override the default distro resource attributes
        .containsEntry(TELEMETRY_DISTRO_NAME.getKey(), attributeValue("elastic"))
        .containsKey(TELEMETRY_DISTRO_VERSION.getKey());
  }

  @AfterAll
  static void end() {
    stopApp();
  }
}
