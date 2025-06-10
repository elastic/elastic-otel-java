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
package co.elastic.opamp.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.elastic.opamp.client.response.MessageData;
import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import opamp.proto.Opamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CentralConfigurationManagerTest {
  private CentralConfigurationManagerImpl centralConfigurationManager;
  private OpampClient client;

  @BeforeEach
  void setUp() {
    client = mock();
    centralConfigurationManager = new CentralConfigurationManagerImpl(client);
  }

  @Test
  void verifyStart() {
    centralConfigurationManager.start(mock());

    verify(client).start(centralConfigurationManager);
  }

  @Test
  void verifyStop() {
    centralConfigurationManager.stop();

    verify(client).stop();
  }

  @Test
  void verifyConfigProcessing_onSuccess() {
    CentralConfigurationProcessor processor = mock();
    String centralConfigValue =
        "{\"transaction_max_spans\":\"200\",\"something_else\":\"some value\"}";
    MessageData messageData =
        createMessageDataWithRemoteConfig(centralConfigValue.getBytes(StandardCharsets.UTF_8));
    centralConfigurationManager.start(processor);
    doReturn(CentralConfigurationProcessor.Result.SUCCESS).when(processor).process(anyMap());

    centralConfigurationManager.onMessage(client, messageData);

    // Verify processed map:
    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    verify(processor).process(captor.capture());
    Map<String, String> providedMap = captor.getValue();
    assertThat(providedMap)
        .containsExactly(
            entry("transaction_max_spans", "200"), entry("something_else", "some value"));

    // Verify opamp client communication:
    ArgumentCaptor<Opamp.RemoteConfigStatus> statusCaptor =
        ArgumentCaptor.forClass(Opamp.RemoteConfigStatus.class);
    verify(client).setRemoteConfigStatus(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getStatus())
        .isEqualTo(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLIED);
  }

  @Test
  void verifyConfigProcessing_onFailure() {
    CentralConfigurationProcessor processor = mock();
    String centralConfigValue =
        "{\"transaction_max_spans\":\"200\",\"something_else\":\"some value\"}";
    MessageData messageData =
        createMessageDataWithRemoteConfig(centralConfigValue.getBytes(StandardCharsets.UTF_8));
    centralConfigurationManager.start(processor);
    doReturn(CentralConfigurationProcessor.Result.FAILURE).when(processor).process(anyMap());

    centralConfigurationManager.onMessage(client, messageData);

    // Verify processed map:
    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    verify(processor).process(captor.capture());
    Map<String, String> providedMap = captor.getValue();
    assertThat(providedMap)
        .containsExactly(
            entry("transaction_max_spans", "200"), entry("something_else", "some value"));

    // Verify opamp client communication:
    ArgumentCaptor<Opamp.RemoteConfigStatus> statusCaptor =
        ArgumentCaptor.forClass(Opamp.RemoteConfigStatus.class);
    verify(client).setRemoteConfigStatus(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getStatus())
        .isEqualTo(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
  }

  @Test
  void verifyConfigProcessing_whenThereIsAParsingError() {
    CentralConfigurationProcessor processor = mock();
    String centralConfigValue = "{invalid:\"json\"";
    MessageData messageData =
        createMessageDataWithRemoteConfig(centralConfigValue.getBytes(StandardCharsets.UTF_8));
    centralConfigurationManager.start(processor);
    doReturn(CentralConfigurationProcessor.Result.FAILURE).when(processor).process(anyMap());

    centralConfigurationManager.onMessage(client, messageData);

    verify(processor, never()).process(anyMap());

    // Verify opamp client communication:
    ArgumentCaptor<Opamp.RemoteConfigStatus> statusCaptor =
        ArgumentCaptor.forClass(Opamp.RemoteConfigStatus.class);
    verify(client).setRemoteConfigStatus(statusCaptor.capture());
    assertThat(statusCaptor.getValue().getStatus())
        .isEqualTo(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_FAILED);
  }

  private MessageData createMessageDataWithRemoteConfig(byte[] configBody) {
    Opamp.AgentConfigFile agentConfigFile =
        Opamp.AgentConfigFile.newBuilder()
            .setContentType("application/json")
            .setBody(ByteString.copyFrom(configBody))
            .build();

    Opamp.AgentConfigMap configMap =
        Opamp.AgentConfigMap.newBuilder().putConfigMap("elastic", agentConfigFile).build();

    return MessageData.builder()
        .setRemoteConfig(Opamp.AgentRemoteConfig.newBuilder().setConfig(configMap).build())
        .build();
  }
}
