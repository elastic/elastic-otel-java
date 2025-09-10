package co.elastic.otel.dynamicconfig.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.StartStop;
import okio.Buffer;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.AgentToServer;
import opamp.proto.RemoteConfigStatuses;
import opamp.proto.ServerToAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpampManagerTest {
  private OpampManager opampManager;
  @StartStop public final MockWebServer server = new MockWebServer();

  @BeforeEach
  void setUp() {
    opampManager =
        OpampManager.builder().setConfigurationEndpoint(server.url("/").toString()).build();
  }

  @Test
  void verifyConfigProcessing_onSuccess() throws InterruptedException, IOException {
    AtomicReference<Map<String, String>> parsedConfig = new AtomicReference<>();
    CountDownLatch configAvailable = new CountDownLatch(1);
    OpampManager.CentralConfigurationProcessor processor =
        (config) -> {
          parsedConfig.set(config);
          configAvailable.countDown();
          return OpampManager.CentralConfigurationProcessor.Result.SUCCESS;
        };
    String centralConfigValue =
        "{\"transaction_max_spans\":\"200\",\"something_else\":\"some value\"}";
    server.enqueue(
        createMockResponse(createServerToAgentWithCentralConfig(centralConfigValue, "some_hash")));
    server.enqueue(new MockResponse.Builder().build());

    opampManager.start(processor);

    // Await for first poll request
    takeRequestOrFail();
    configAvailable.await();

    // Verify parsed config from server response:
    assertThat(parsedConfig.get())
        .containsExactly(
            entry("transaction_max_spans", "200"), entry("something_else", "some value"));

    // Verify opamp client communication:
    RecordedRequest statusUpdateRequest = takeRequestOrFail();
    AgentToServer agentToServer = getAgentToServerMessage(statusUpdateRequest);
    assertThat(agentToServer.remote_config_status.status)
        .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
    assertThat(
            agentToServer.remote_config_status.last_remote_config_hash.string(
                StandardCharsets.UTF_8))
        .isEqualTo("some_hash");
  }

  @Test
  void verifyConfigProcessing_onFailure() throws InterruptedException, IOException {
    OpampManager.CentralConfigurationProcessor processor =
        (config) -> OpampManager.CentralConfigurationProcessor.Result.FAILURE;
    String centralConfigValue =
        "{\"transaction_max_spans\":\"200\",\"something_else\":\"some value\"}";
    server.enqueue(
        createMockResponse(createServerToAgentWithCentralConfig(centralConfigValue, "some_hash")));
    server.enqueue(new MockResponse.Builder().build());

    opampManager.start(processor);

    // Await for first poll request
    takeRequestOrFail();

    // Verify opamp client communication:
    RecordedRequest statusUpdateRequest = takeRequestOrFail();
    AgentToServer agentToServer = getAgentToServerMessage(statusUpdateRequest);
    assertThat(agentToServer.remote_config_status.status)
        .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
  }

  @Test
  void verifyConfigProcessing_whenThereIsAParsingError() throws InterruptedException, IOException {
    OpampManager.CentralConfigurationProcessor processor =
        (config) -> OpampManager.CentralConfigurationProcessor.Result.FAILURE;
    String centralConfigValue = "{invalid:\"json\"";
    server.enqueue(
        createMockResponse(createServerToAgentWithCentralConfig(centralConfigValue, "some_hash")));
    server.enqueue(new MockResponse.Builder().build());

    opampManager.start(processor);

    // Await for first poll request
    takeRequestOrFail();

    // Verify opamp client communication:
    RecordedRequest statusUpdateRequest = takeRequestOrFail();
    AgentToServer agentToServer = getAgentToServerMessage(statusUpdateRequest);
    assertThat(agentToServer.remote_config_status.status)
        .isEqualTo(RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
  }

  private MockResponse createMockResponse(ServerToAgent serverToAgent) {
    Buffer buffer = new Buffer();
    buffer.write(serverToAgent.encodeByteString().toByteArray());
    return new MockResponse.Builder().code(200).body(buffer).build();
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

  private RecordedRequest takeRequestOrFail() throws InterruptedException {
    RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
    if (recordedRequest == null) {
      fail();
    }
    return recordedRequest;
  }

  private static AgentToServer getAgentToServerMessage(RecordedRequest statusUpdateRequest)
      throws IOException {
    return AgentToServer.ADAPTER.decode(statusUpdateRequest.getBody().toByteArray());
  }
}
