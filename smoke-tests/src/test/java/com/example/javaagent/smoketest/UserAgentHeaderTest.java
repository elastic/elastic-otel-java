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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

public class UserAgentHeaderTest {

  private static final Logger logger = LoggerFactory.getLogger(UserAgentHeaderTest.class);

  private static final String AGENT_VERSION = extractVersion(SmokeTest.AGENT_PATH);

  private static WireMockServer wireMock;

  @BeforeAll
  public static void startWireMock() {
    wireMock = new WireMockServer();
    wireMock.start();
  }

  @AfterAll
  public static void stopWireMock() {
    wireMock.stop();
  }

  @AfterEach
  public void resetWiremock() {
    wireMock.resetAll();
  }

  private String wiremockHostFromContainer() {
    return "host.testcontainers.internal:" + wireMock.port();
  }

  @Test
  public void verifyHttpExporterAgentHeaders() {
    wireMock.stubFor(
        post(urlEqualTo("/v1/traces"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));

    wireMock.stubFor(
        post(urlEqualTo("/v1/metrics"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));

    wireMock.stubFor(
        post(urlEqualTo("/v1/logs"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));

    try (GenericContainer<?> container =
        runTestApp(
            cont ->
                cont.withEnv("OTEL_EXPORTER_OTLP_PROTOCOL", "http/protobuf")
                    .withEnv(
                        "OTEL_EXPORTER_OTLP_ENDPOINT", "http://" + wiremockHostFromContainer()))) {

      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(
              () -> {
                // Traces ands logs are generated via the container health check
                verify(
                    postRequestedFor(urlEqualTo("/v1/traces"))
                        .withHeader(
                            "User-Agent", equalTo("elastic-otlp-http-java/" + AGENT_VERSION)));
                verify(
                    postRequestedFor(urlEqualTo("/v1/metrics"))
                        .withHeader(
                            "User-Agent", equalTo("elastic-otlp-http-java/" + AGENT_VERSION)));
                verify(
                    postRequestedFor(urlEqualTo("/v1/logs"))
                        .withHeader(
                            "User-Agent", equalTo("elastic-otlp-http-java/" + AGENT_VERSION)));
              });
    }
  }

  @Test
  public void verifyGrpcExporterAgentHeaders() {

    // These responses are not correct for GRPC, but we don't care - we only care about the request
    // headers
    wireMock.stubFor(
        post(urlEqualTo("/opentelemetry.proto.collector.trace.v1.TraceService/Export"))
            .willReturn(aResponse().withStatus(200)));
    wireMock.stubFor(
        post(urlEqualTo("/opentelemetry.proto.collector.metrics.v1.MetricsService/Export"))
            .willReturn(aResponse().withStatus(200)));
    wireMock.stubFor(
        post(urlEqualTo("/opentelemetry.proto.collector.logs.v1.LogsService/Export"))
            .willReturn(aResponse().withStatus(200)));

    try (GenericContainer<?> container =
        runTestApp(
            cont ->
                cont.withEnv("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")
                    .withEnv(
                        "OTEL_EXPORTER_OTLP_ENDPOINT", "http://" + wiremockHostFromContainer()))) {

      await()
          .atMost(Duration.ofSeconds(10))
          .untilAsserted(
              () -> {
                // Traces ands logs are generated via the container health check
                verify(
                    postRequestedFor(
                            urlEqualTo(
                                "/opentelemetry.proto.collector.trace.v1.TraceService/Export"))
                        .withHeader(
                            "User-Agent", equalTo("elastic-otlp-grpc-java/" + AGENT_VERSION)));
                verify(
                    postRequestedFor(
                            urlEqualTo(
                                "/opentelemetry.proto.collector.metrics.v1.MetricsService/Export"))
                        .withHeader(
                            "User-Agent", equalTo("elastic-otlp-grpc-java/" + AGENT_VERSION)));
                verify(
                    postRequestedFor(
                            urlEqualTo("/opentelemetry.proto.collector.logs.v1.LogsService/Export"))
                        .withHeader(
                            "User-Agent", equalTo("elastic-otlp-grpc-java/" + AGENT_VERSION)));
              });
    }
  }

  private GenericContainer<?> runTestApp(Consumer<GenericContainer<?>> customizer) {
    Testcontainers.exposeHostPorts(wireMock.port());
    GenericContainer<?> target =
        new GenericContainer<>(TestAppSmokeTest.TEST_APP_IMAGE)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(MountableFile.forHostPath(SmokeTest.AGENT_PATH), "/agent.jar")
            .withEnv("JAVA_TOOL_OPTIONS", JavaExecutable.jvmAgentArgument("/agent.jar"))
            // speed up exports
            .withEnv("OTEL_BSP_MAX_EXPORT_BATCH", "1")
            .withEnv("OTEL_BSP_SCHEDULE_DELAY", "10")
            .withEnv("OTEL_BLRP_MAX_EXPORT_BATCH", "1")
            .withEnv("OTEL_BLRP_SCHEDULE_DELAY", "10")
            .withEnv("OTEL_METRIC_EXPORT_INTERVAL", "10")
            // use grpc endpoint as default is now http/protobuf with agent 2.x
            .withExposedPorts(TestAppSmokeTest.PORT)
            .waitingFor(Wait.forHttp("/health").forPort(TestAppSmokeTest.PORT));
    customizer.accept(target);
    target.start();
    return target;
  }

  private static String extractVersion(String agentJarPath) {
    try (JarInputStream jarStream = new JarInputStream(new FileInputStream(agentJarPath))) {
      Manifest mf = jarStream.getManifest();
      Attributes attributes = mf.getMainAttributes();
      return Objects.requireNonNull(attributes.getValue("Implementation-Version"));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
