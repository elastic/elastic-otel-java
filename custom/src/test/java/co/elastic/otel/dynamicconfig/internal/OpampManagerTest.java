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
package co.elastic.otel.dynamicconfig.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.AgentToServer;
import opamp.proto.RemoteConfigStatuses;
import opamp.proto.ServerToAgent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
class OpampManagerTest {
  private OpampManager opampManager;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
    opampManager =
        OpampManager.builder().setConfigurationEndpoint(wmRuntimeInfo.getHttpBaseUrl()).build();
  }

  @Test
  void verifyConfigProcessing_onSuccess() throws IOException {
    AtomicReference<Map<String, String>> parsedConfig = new AtomicReference<>();
    OpampManager.CentralConfigurationProcessor processor =
        (config) -> {
          parsedConfig.set(config);
          return OpampManager.CentralConfigurationProcessor.Result.SUCCESS;
        };
    String centralConfigValue =
        "{\"transaction_max_spans\":\"200\",\"something_else\":\"some value\"}";
    stubFor(
        any(anyUrl())
            .inScenario("opamp")
            .whenScenarioStateIs(STARTED)
            .willReturn(
                ok().withBody(
                        createServerToAgentWithCentralConfig(centralConfigValue, "some_hash")
                            .encodeByteString()
                            .toByteArray()))
            .willSetStateTo("status_update"));
    stubFor(
        any(anyUrl()).inScenario("opamp").whenScenarioStateIs("status_update").willReturn(ok()));

    opampManager.start(processor);

    // Await for server requests
    List<LoggedRequest> requests = awaitAndGetLoggedRequestsInOrder(2);

    // Verify parsed config from server response:
    assertThat(parsedConfig.get())
        .containsExactly(
            entry("transaction_max_spans", "200"), entry("something_else", "some value"));

    // Verify opamp client communication:
    Request statusUpdateRequest = requests.get(1);
    AgentToServer agentToServer = getAgentToServerMessage(statusUpdateRequest);
    assertThat(agentToServer.remote_config_status.status)
        .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
    assertThat(
            agentToServer.remote_config_status.last_remote_config_hash.string(
                StandardCharsets.UTF_8))
        .isEqualTo("some_hash");
  }

  @Test
  void verifyConfigProcessing_onFailure() throws IOException {
    OpampManager.CentralConfigurationProcessor processor =
        (config) -> OpampManager.CentralConfigurationProcessor.Result.FAILURE;
    String centralConfigValue =
        "{\"transaction_max_spans\":\"200\",\"something_else\":\"some value\"}";
    stubFor(
        any(anyUrl())
            .inScenario("opamp")
            .whenScenarioStateIs(STARTED)
            .willReturn(
                ok().withBody(
                        createServerToAgentWithCentralConfig(centralConfigValue, "some_hash")
                            .encodeByteString()
                            .toByteArray()))
            .willSetStateTo("status_update"));
    stubFor(
        any(anyUrl()).inScenario("opamp").whenScenarioStateIs("status_update").willReturn(ok()));

    opampManager.start(processor);

    // Await for requests
    List<LoggedRequest> requests = awaitAndGetLoggedRequestsInOrder(2);

    // Verify opamp client communication:
    LoggedRequest statusUpdateRequest = requests.get(1);
    AgentToServer agentToServer = getAgentToServerMessage(statusUpdateRequest);
    assertThat(agentToServer.remote_config_status.status)
        .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
  }

  @Test
  void verifyConfigProcessing_whenThereIsAParsingError() throws IOException {
    OpampManager.CentralConfigurationProcessor processor =
        (config) -> OpampManager.CentralConfigurationProcessor.Result.FAILURE;
    String centralConfigValue = "{invalid:\"json\"";
    stubFor(
        any(anyUrl())
            .inScenario("opamp")
            .whenScenarioStateIs(STARTED)
            .willReturn(
                ok().withBody(
                        createServerToAgentWithCentralConfig(centralConfigValue, "some_hash")
                            .encodeByteString()
                            .toByteArray()))
            .willSetStateTo("status_update"));
    stubFor(
        any(anyUrl()).inScenario("opamp").whenScenarioStateIs("status_update").willReturn(ok()));

    opampManager.start(processor);

    // Await for requests
    List<LoggedRequest> requests = awaitAndGetLoggedRequestsInOrder(2);

    // Verify opamp client communication:
    LoggedRequest statusUpdateRequest = requests.get(1);
    AgentToServer agentToServer = getAgentToServerMessage(statusUpdateRequest);
    assertThat(agentToServer.remote_config_status.status)
        .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
  }

  @Test
  void verifyRetry_ExponentialBackoff(WireMockRuntimeInfo wmRuntimeInfo)
      throws InterruptedException {
    OpampManager.CentralConfigurationProcessor processor =
        (config) -> OpampManager.CentralConfigurationProcessor.Result.FAILURE;
    stubFor(any(anyUrl()).willReturn(aResponse().withStatus(503)));

    // Set up manager with small polling interval
    opampManager =
        OpampManager.builder()
            .setConfigurationEndpoint(wmRuntimeInfo.getHttpBaseUrl())
            .setPollingInterval(Duration.ofSeconds(1))
            .build();
    long initialTimeNanos = System.nanoTime();
    opampManager.start(processor);

    // |--timelineInSeconds--|0--------1--------------3---------------------7-----------------------------15
    // |------requests-------|--first--|----second----|--------third--------|------------fourth-----------|
    List<Long> requestExpectedAfterTimeInNanos = new ArrayList<>();
    // First request happens after the 0s mark.
    requestExpectedAfterTimeInNanos.add(TimeUnit.SECONDS.toNanos(0));
    // Second request happens after the 1s mark.
    requestExpectedAfterTimeInNanos.add(TimeUnit.SECONDS.toNanos(1));
    // Third request happens after the 3s mark.
    requestExpectedAfterTimeInNanos.add(TimeUnit.SECONDS.toNanos(3));
    // Fourth request happens after the 7s mark.
    requestExpectedAfterTimeInNanos.add(TimeUnit.SECONDS.toNanos(7));

    sleep(TimeUnit.SECONDS.toMillis(10));
    List<LoggedRequest> requests = getLoggedRequestsInOrder();

    // Only 4 requests should be recorded in a span of 10s with exponential delay starting with 1s.
    assertThat(requests).hasSize(4);
    // Verify request times
    for (int i = 0; i < requestExpectedAfterTimeInNanos.size(); i++) {
      LoggedRequest request = requests.get(i);
      long requestTimeNanos = TimeUnit.MILLISECONDS.toNanos(request.getLoggedDate().getTime());
      Long expectedAfterTimeNanos = requestExpectedAfterTimeInNanos.get(i);
      assertThat(requestTimeNanos - initialTimeNanos).isGreaterThan(expectedAfterTimeNanos);
    }
  }

  private ServerToAgent createServerToAgentWithCentralConfig(String centralConfig, String hash) {
    AgentRemoteConfig remoteConfig =
        new AgentRemoteConfig.Builder()
            .config(createAgentConfigMap(centralConfig))
            .config_hash(ByteString.of(hash.getBytes(StandardCharsets.UTF_8)))
            .build();

    return new ServerToAgent.Builder().remote_config(remoteConfig).build();
  }

  private AgentConfigMap createAgentConfigMap(String configBody) {
    AgentConfigFile agentConfigFile =
        new AgentConfigFile.Builder()
            .content_type("application/json")
            .body(ByteString.of(configBody.getBytes(StandardCharsets.UTF_8)))
            .build();

    Map<String, AgentConfigFile> map = new HashMap<>();
    map.put("elastic", agentConfigFile);
    return new AgentConfigMap.Builder().config_map(map).build();
  }

  private static AgentToServer getAgentToServerMessage(Request statusUpdateRequest)
      throws IOException {
    return AgentToServer.ADAPTER.decode(statusUpdateRequest.getBody());
  }

  private static List<LoggedRequest> awaitAndGetLoggedRequestsInOrder(int expectedRequests) {
    return awaitAndGetLoggedRequestsInOrder(expectedRequests, Duration.ofSeconds(1));
  }

  private static List<LoggedRequest> awaitAndGetLoggedRequestsInOrder(
      int expectedRequests, Duration awaitForExpectedRequests) {
    await()
        .atMost(awaitForExpectedRequests)
        .until(() -> getAllServeEvents().size() == expectedRequests);

    return getLoggedRequestsInOrder();
  }

  @NotNull
  private static List<LoggedRequest> getLoggedRequestsInOrder() {
    List<LoggedRequest> requests = new ArrayList<>();
    for (ServeEvent serveEvent : getAllServeEvents()) {
      requests.add(serveEvent.getRequest());
    }
    requests.sort(Comparator.comparing(LoggedRequest::getLoggedDate));
    return requests;
  }
}
