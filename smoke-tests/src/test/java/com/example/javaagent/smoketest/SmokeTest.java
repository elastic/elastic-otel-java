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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

abstract class SmokeTest {
  private static final Logger logger = LoggerFactory.getLogger(SmokeTest.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final int TARGET_DEBUG_PORT = 5005;
  private static final int BACKEND_DEBUG_PORT = 5006;
  private static final String JAVAAGENT_JAR_PATH = "/opentelemetry-javaagent.jar";

  protected static final String MOCK_SERVER_HOST = "mock-server";
  // map to HTTPS port for kubernetes, mock server will handle both http and https on the same port
  protected static final int MOCK_SERVER_PORT = 443;

  protected static OkHttpClient client = OkHttpUtils.client();

  private static final Network network = Network.newNetwork();
  public static final String AGENT_PATH =
      System.getProperty("io.opentelemetry.smoketest.agent.shadowJar.path");

  // keep track of all started containers in case they aren't properly stopped
  private static final List<GenericContainer<?>> startedContainers = new ArrayList<>();

  private static GenericContainer<?> backend;

  @BeforeAll
  @SuppressWarnings("resource")
  static void setupSpec() {
    backend =
        new GenericContainer<>(
                "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-fake-backend:20221127.3559314891")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/health").forPort(8080))
            .withNetwork(network)
            .withNetworkAliases("backend")
            .withLogConsumer(new Slf4jLogConsumer(logger));

    if (JavaExecutable.isDebugging()
        && JavaExecutable.isListeningDebuggerStarted(BACKEND_DEBUG_PORT, "backend")) {
      backend.withEnv(
          "JAVA_TOOL_OPTIONS",
          JavaExecutable.jvmDebugArgument("remote-localhost", BACKEND_DEBUG_PORT));
      backend = addDockerDebugHost(backend);
    }

    backend.start();
  }

  protected static GenericContainer<?> startTarget(
      String image, Consumer<GenericContainer<?>> customizeContainer) {

    @SuppressWarnings("resource")
    GenericContainer<?> target =
        new GenericContainer<>(image)
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withCopyFileToContainer(MountableFile.forHostPath(AGENT_PATH), JAVAAGENT_JAR_PATH)

            // batch span processor: very small batch size for testing
            .withEnv("OTEL_BSP_MAX_EXPORT_BATCH", "1")
            // batch span processor: very short delay for testing
            .withEnv("OTEL_BSP_SCHEDULE_DELAY", "10")
            // use grpc endpoint as default is now http/protobuf with agent 2.x
            .withEnv("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")
            .withEnv("OTEL_PROPAGATORS", "tracecontext,baggage")
            .withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://backend:8080");

    StringBuilder jvmArgs = new StringBuilder();

    if (JavaExecutable.isDebugging()) {
      // when debugging, automatically use verbose debug logging
      target.withEnv("OTEL_JAVAAGENT_DEBUG", "true");

      if (JavaExecutable.isListeningDebuggerStarted(TARGET_DEBUG_PORT, "target")) {
        target = addDockerDebugHost(target);
        jvmArgs
            .append(JavaExecutable.jvmDebugArgument("remote-localhost", TARGET_DEBUG_PORT))
            .append(" ");
      }
      // Use very long startup delay when debugging as the remote JVM is likely stopped before the
      // app has started
      target.waitingFor(Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofMinutes(10)));
    }

    jvmArgs.append(JavaExecutable.jvmAgentArgument(JAVAAGENT_JAR_PATH));
    target.withEnv("JAVA_TOOL_OPTIONS", jvmArgs.toString());

    customizeContainer.accept(target);

    Objects.requireNonNull(target).start();

    startedContainers.add(target);

    return target;
  }

  private static GenericContainer<?> addDockerDebugHost(GenericContainer<?> target) {
    // make the docker host IP available for remote debug
    // the 'host-gateway' is automatically translated by docker for all OSes
    target = target.withExtraHost("remote-localhost", "host-gateway");
    return target;
  }

  @BeforeEach
  void beforeEach() throws IOException {
    // because traces reporting is asynchronous we need to wait for the healthcheck traces to be
    // reported and only then
    // flush before the test, otherwise the first test will see the healthcheck trace captured.
    waitForTraces();
    clearBackend();
  }

  protected static GenericContainer<?> startMockServer() {
    return startMockServer((container) -> {});
  }

  protected static GenericContainer<?> startMockServer(
      Consumer<GenericContainer<?>> customizeContainer) {
    @SuppressWarnings("resource")
    GenericContainer<?> target =
        new GenericContainer<>("mockserver/mockserver:5.15.0")
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withNetworkAliases(MOCK_SERVER_HOST)
            .withExposedPorts(MOCK_SERVER_PORT)
            .withEnv("SERVER_PORT", Integer.toString(MOCK_SERVER_PORT))
            .waitingFor(Wait.forHttp("/mockserver/status").withMethod("PUT"));

    // only use mock server verbose output when debugging
    String logLevel = "WARN";
    if (JavaExecutable.isDebugging() || JavaExecutable.isDebugInCI()) {
      logLevel = "INFO";
    }
    target.withEnv("JAVA_TOOL_OPTIONS", "-Dmockserver.logLevel=" + logLevel);

    customizeContainer.accept(target);

    Objects.requireNonNull(target).start();

    startedContainers.add(target);

    return target;
  }

  protected static void clearBackend() throws IOException {
    client
        .newCall(
            new Request.Builder()
                .url(String.format("http://localhost:%d/clear", backend.getMappedPort(8080)))
                .build())
        .execute()
        .close();
  }

  @AfterAll
  static void afterAll() {
    backend.stop();

    startedContainers.forEach(
        c -> {
          if (c.isRunning()) {
            logger.warn(
                "test container not properly terminated by test : {} {}",
                c.getImage(),
                c.getContainerId());
            c.stop();
          }
        });
  }

  protected static String getUrl(GenericContainer<?> target, String path, int port) {
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException("path must start with '/'");
    }
    return String.format("http://localhost:%d%s", target.getMappedPort(port), path);
  }

  protected static void checkTracesResources(
      ResourceAttributesCheck check, List<ExportTraceServiceRequest> traces) {

    assertThat(traces).describedAs("at least one exported trace expected").isNotEmpty();

    List<Resource> resources =
        traces.stream()
            .flatMap(t -> t.getResourceSpansList().stream())
            .map(ResourceSpans::getResource)
            .collect(Collectors.toList());

    assertThat(resources).describedAs("traces resources must not be empty").isNotEmpty();

    resources.forEach(
        resource -> check.verify(assertThat(getAttributes(resource.getAttributesList()))));
  }

  protected interface ResourceAttributesCheck {
    void verify(MapAssert<String, AnyValue> attributes);
  }

  protected static int countResourcesByValue(
      Collection<ExportTraceServiceRequest> traces, String resourceName, String value) {
    return (int)
        traces.stream()
            .flatMap(it -> it.getResourceSpansList().stream())
            .flatMap(it -> it.getResource().getAttributesList().stream())
            .filter(
                kv ->
                    kv.getKey().equals(resourceName)
                        && kv.getValue().getStringValue().equals(value))
            .count();
  }

  protected static int countSpansByName(
      Collection<ExportTraceServiceRequest> traces, String spanName) {
    return (int) getSpanStream(traces).filter(it -> it.getName().equals(spanName)).count();
  }

  protected static int countSpansByAttributeValue(
      Collection<ExportTraceServiceRequest> traces, String attributeName, String attributeValue) {
    return (int)
        getSpanStream(traces)
            .flatMap(it -> it.getAttributesList().stream())
            .filter(
                kv ->
                    kv.getKey().equals(attributeName)
                        && kv.getValue().getStringValue().equals(attributeValue))
            .count();
  }

  protected static Stream<Span> getSpanStream(Collection<ExportTraceServiceRequest> traces) {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .flatMap(it -> it.getScopeSpansList().stream())
        .flatMap(it -> it.getSpansList().stream());
  }

  protected List<ExportTraceServiceRequest> waitForTraces() {
    try {
      String content = waitForContent();

      return StreamSupport.stream(OBJECT_MAPPER.readTree(content).spliterator(), false)
          .map(
              it -> {
                ExportTraceServiceRequest.Builder builder = ExportTraceServiceRequest.newBuilder();
                // TODO(anuraaga): Register parser into object mapper to avoid de -> re ->
                // deserialize.
                try {
                  JsonFormat.parser().merge(OBJECT_MAPPER.writeValueAsString(it), builder);
                } catch (InvalidProtocolBufferException | JsonProcessingException e) {
                  e.printStackTrace();
                }
                return builder.build();
              })
          .collect(Collectors.toList());

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String waitForContent() {
    long previousSize = 0;
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
    String content = "[]";

    Request request =
        new Request.Builder()
            .url(String.format("http://localhost:%d/get-traces", backend.getMappedPort(8080)))
            .build();

    while (System.currentTimeMillis() < deadline) {
      try (ResponseBody body = client.newCall(request).execute().body()) {
        content = body.string();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (content.length() > 2 && content.length() == previousSize) {
        break;
      }
      previousSize = content.length();
      try {
        TimeUnit.MILLISECONDS.sleep(500);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    return content;
  }

  protected static AnyValue attributeValue(boolean value) {
    return AnyValue.newBuilder().setBoolValue(value).build();
  }

  protected static AnyValue attributeValue(String value) {
    return AnyValue.newBuilder().setStringValue(value).build();
  }

  protected static AnyValue attributeArrayValue(String... values){
    ArrayValue.Builder valueBuilder = ArrayValue.newBuilder();
    for (String value : values) {
      valueBuilder.addValues(AnyValue.newBuilder().setStringValue(value).build());
    }
    return AnyValue.newBuilder().setArrayValue(valueBuilder.build()).build();
  }

  protected static Map<String, AnyValue> getAttributes(List<KeyValue> list) {
    Map<String, AnyValue> attributes = new HashMap<>();
    for (KeyValue kv : list) {
      attributes.put(kv.getKey(), kv.getValue());
    }
    return attributes;
  }

  protected Stream<Span> getSpans(List<ExportTraceServiceRequest> traces) {
    return traces.stream()
        .flatMap(it -> it.getResourceSpansList().stream())
        .flatMap(it -> it.getScopeSpansList().stream())
        .flatMap(it -> it.getSpansList().stream());
  }

  protected static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      String part = Integer.toHexString((bytes[i] & 0xFF));
      if (part.length() < 2) {
        sb.append('0');
      }
      sb.append(part);
    }
    return sb.toString();
  }
  protected void doRequest(String url, IOConsumer<Response> responseHandler) {
    doRequest(url, Collections.emptyMap(), responseHandler);
  }

  protected void doRequest(String url, Map<String,String> headers, IOConsumer<Response> responseHandler) {
    Request.Builder request = new Request.Builder().url(url).get();
    headers.forEach(request::addHeader);

      try (Response response = client.newCall(request.build()).execute()) {
        responseHandler.accept(response);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @FunctionalInterface
    protected interface IOConsumer<T> {
      void accept(T t) throws IOException;
    }

    protected static IOConsumer<Response> okResponse() {
      return r -> {
        assertThat(r.code()).isEqualTo(200);
      };
    }

    protected static IOConsumer<Response> okResponseBody(String body) {
      return r -> {
        assertThat(r.code()).isEqualTo(200);
        assertThat(r.body()).isNotNull();
        assertThat(r.body().string()).isEqualTo(body);
      };
    }
  }
