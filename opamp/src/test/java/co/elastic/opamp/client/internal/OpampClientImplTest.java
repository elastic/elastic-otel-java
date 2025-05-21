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
package co.elastic.opamp.client.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.elastic.opamp.client.OpampClient;
import co.elastic.opamp.client.internal.state.AgentDescriptionState;
import co.elastic.opamp.client.internal.state.CapabilitiesState;
import co.elastic.opamp.client.internal.state.InstanceUidState;
import co.elastic.opamp.client.internal.state.OpampClientState;
import co.elastic.opamp.client.internal.state.RemoteConfigStatusState;
import co.elastic.opamp.client.internal.state.SequenceNumberState;
import co.elastic.opamp.client.request.Request;
import co.elastic.opamp.client.request.service.RequestService;
import co.elastic.opamp.client.response.MessageData;
import co.elastic.opamp.client.response.Response;
import co.elastic.opamp.client.state.State;
import com.google.protobuf.ByteString;
import opamp.proto.Opamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpampClientImplTest {
  @Mock private RequestService requestService;
  @Mock private OpampClient.Callback callback;
  private OpampClientState state;
  private OpampClientImpl client;

  @BeforeEach
  void setUp() {
    state =
        new OpampClientState(
            RemoteConfigStatusState.create(),
            SequenceNumberState.create(),
            AgentDescriptionState.create(),
            CapabilitiesState.create(),
            InstanceUidState.createRandom(),
            State.createInMemory(Opamp.EffectiveConfig.newBuilder().build()));
    client = OpampClientImpl.create(requestService, state);
  }

  @Test
  void verifyStart() {
    client.start(callback);

    verify(requestService).start(client, client);
  }

  @Test
  void verifyStop() {
    client.start(callback);
    verify(requestService).start(client, client);

    client.stop();
    verify(requestService).stop();
  }

  @Test
  void verifyStartOnlyOnce() {
    client.start(callback);

    try {
      client.start(callback);
      fail("Should have thrown an exception");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The client has already been started");
    }
  }

  @Test
  void verifyStopOnlyOnce() {
    client.start(callback);

    client.stop();

    try {
      client.stop();
      fail("Should have thrown an exception");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The client has already been stopped");
    }
  }

  @Test
  void verifyStopOnlyAfterStart() {
    try {
      client.stop();
      fail("Should have thrown an exception");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The client has not been started");
    }
  }

  @Test
  void checkRequestFields() {
    client.start(callback);

    Opamp.AgentToServer firstRequest = client.get().getAgentToServer();

    assertThat(firstRequest.getInstanceUid().toByteArray()).isEqualTo(state.instanceUidState.get());
    assertThat(firstRequest.getSequenceNum()).isEqualTo(1);
    assertThat(firstRequest.getCapabilities()).isEqualTo(state.capabilitiesState.get());
    assertThat(firstRequest.hasAgentDescription()).isTrue();
    assertThat(firstRequest.hasEffectiveConfig()).isTrue();
    assertThat(firstRequest.hasRemoteConfigStatus()).isTrue();

    client.onRequestSuccess(null);

    // Second request
    Opamp.AgentToServer secondRequest = client.get().getAgentToServer();

    assertThat(secondRequest.getInstanceUid().toByteArray())
        .isEqualTo(state.instanceUidState.get());
    assertThat(secondRequest.getSequenceNum()).isEqualTo(2);
    assertThat(secondRequest.getCapabilities()).isEqualTo(state.capabilitiesState.get());
    assertThat(secondRequest.hasAgentDescription()).isFalse();
    assertThat(secondRequest.hasEffectiveConfig()).isFalse();
    assertThat(secondRequest.hasRemoteConfigStatus()).isFalse();
  }

  @Test
  void verifyRequestBuildingAfterStopIsCalled() {
    client.start(callback);

    client.stop();

    Request request = client.get();
    assertThat(request.getAgentToServer().getAgentDisconnect()).isNotNull();
  }

  @Test
  void onSuccess_withNoChangesToReport_doNotNotifyCallbackOnMessage() {
    Opamp.ServerToAgent serverToAgent = Opamp.ServerToAgent.getDefaultInstance();
    client.start(callback);

    client.onRequestSuccess(Response.create(serverToAgent));

    verify(callback, never()).onMessage(any(), any());
  }

  @Test
  void onSuccessfulResponse_withRemoteConfigStatusUpdate_notifyServerImmediately() {
    Opamp.ServerToAgent response =
        Opamp.ServerToAgent.newBuilder()
            .setRemoteConfig(
                Opamp.AgentRemoteConfig.newBuilder().setConfig(getAgentConfigMap("fileName", "{}")))
            .build();
    TestCallback testCallback =
        new TestCallback() {
          @Override
          public void onMessage(OpampClient client, MessageData messageData) {
            client.setRemoteConfigStatus(
                getRemoteConfigStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));
          }
        };
    client.start(testCallback);
    clearInvocations(requestService);

    client.onRequestSuccess(Response.create(response));

    verify(requestService).sendRequest();
  }

  @Test
  void onSuccessfulResponse_withRemoteConfigStatus_withoutChange_doNotNotifyServerImmediately() {
    Opamp.ServerToAgent response =
        Opamp.ServerToAgent.newBuilder()
            .setRemoteConfig(
                Opamp.AgentRemoteConfig.newBuilder().setConfig(getAgentConfigMap("fileName", "{}")))
            .build();
    TestCallback testCallback =
        new TestCallback() {
          @Override
          public void onMessage(OpampClient client, MessageData messageData) {
            client.setRemoteConfigStatus(
                getRemoteConfigStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));
          }
        };
    client.setRemoteConfigStatus(
        getRemoteConfigStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));
    client.start(testCallback);
    clearInvocations(requestService);

    client.onRequestSuccess(Response.create(response));

    verify(requestService, never()).sendRequest();
  }

  @Test
  void onConnectionSuccessful_notifyCallback() {
    client.start(callback);

    client.onConnectionSuccess();

    verify(callback).onConnect(client);
    verify(callback, never()).onConnectFailed(any(), any());
  }

  @Test
  void onSuccessfulResponse_withServerErrorData_notifyCallback() {
    client.start(callback);
    Opamp.ServerErrorResponse errorResponse = Opamp.ServerErrorResponse.getDefaultInstance();
    Opamp.ServerToAgent serverToAgent =
        Opamp.ServerToAgent.newBuilder().setErrorResponse(errorResponse).build();

    client.onRequestSuccess(Response.create(serverToAgent));

    verify(callback).onErrorResponse(client, errorResponse);
    verify(callback, never()).onMessage(any(), any());
  }

  @Test
  void onConnectionFailed_notifyCallback() {
    client.start(callback);
    Throwable throwable = mock();

    client.onConnectionFailed(throwable);

    verify(callback).onConnectFailed(client, throwable);
    verify(callback, never()).onConnect(any());
  }

  @Test
  void verifyDisableCompressionWhenRequestedByServer() {
    Opamp.ServerToAgent serverToAgent =
        Opamp.ServerToAgent.newBuilder()
            .setFlags(Opamp.ServerToAgentFlags.ServerToAgentFlags_ReportFullState_VALUE)
            .build();
    client.start(callback);

    // First payload contains compressable fields
    Opamp.AgentToServer firstRequest = client.get().getAgentToServer();
    assertThat(firstRequest.hasAgentDescription()).isTrue();
    assertThat(firstRequest.hasEffectiveConfig()).isTrue();
    assertThat(firstRequest.hasRemoteConfigStatus()).isTrue();

    // Second payload doesn't contain compressable fields
    Opamp.AgentToServer secondRequest = client.get().getAgentToServer();
    assertThat(secondRequest.hasAgentDescription()).isFalse();
    assertThat(secondRequest.hasEffectiveConfig()).isFalse();
    assertThat(secondRequest.hasRemoteConfigStatus()).isFalse();

    // When the server requests a full payload, send them again.
    client.onRequestSuccess(Response.create(serverToAgent));

    Opamp.AgentToServer thirdRequest = client.get().getAgentToServer();
    assertThat(thirdRequest.hasAgentDescription()).isTrue();
    assertThat(thirdRequest.hasEffectiveConfig()).isTrue();
    assertThat(thirdRequest.hasRemoteConfigStatus()).isTrue();
  }

  @Test
  void verifySequenceNumberIncreasesOnServerResponseReceived() {
    client.start(callback);
    assertThat(state.sequenceNumberState.get()).isEqualTo(1);
    Opamp.ServerToAgent serverToAgent = Opamp.ServerToAgent.getDefaultInstance();

    client.onRequestSuccess(Response.create(serverToAgent));

    assertThat(state.sequenceNumberState.get()).isEqualTo(2);
  }

  @Test
  void verifySequenceNumberDoesNotIncreaseOnRequestError() {
    client.start(callback);
    assertThat(state.sequenceNumberState.get()).isEqualTo(1);

    client.onRequestFailed(new Exception());

    assertThat(state.sequenceNumberState.get()).isEqualTo(1);
  }

  @Test
  void whenStatusIsUpdated_notifyServerImmediately() {
    client.setRemoteConfigStatus(
        getRemoteConfigStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_UNSET));
    client.start(callback);
    clearInvocations(requestService);

    client.setRemoteConfigStatus(
        getRemoteConfigStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));

    verify(requestService).sendRequest();
  }

  @Test
  void whenStatusIsNotUpdated_doNotNotifyServerImmediately() {
    client.setRemoteConfigStatus(
        getRemoteConfigStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));
    client.start(callback);
    clearInvocations(requestService);

    client.setRemoteConfigStatus(
        getRemoteConfigStatus(Opamp.RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));

    verify(requestService, never()).sendRequest();
  }

  @Test
  void whenServerProvidesNewInstanceUid_useIt() {
    client.start(callback);
    byte[] serverProvidedUid = new byte[] {1, 2, 3};
    Opamp.ServerToAgent response =
        Opamp.ServerToAgent.newBuilder()
            .setAgentIdentification(
                Opamp.AgentIdentification.newBuilder()
                    .setNewInstanceUid(ByteString.copyFrom(serverProvidedUid))
                    .build())
            .build();

    client.onRequestSuccess(Response.create(response));

    assertThat(state.instanceUidState.get()).isEqualTo(serverProvidedUid);
  }

  private static Opamp.RemoteConfigStatus getRemoteConfigStatus(Opamp.RemoteConfigStatuses status) {
    return Opamp.RemoteConfigStatus.newBuilder().setStatus(status).build();
  }

  private Opamp.AgentConfigMap getAgentConfigMap(String configFileName, String content) {
    Opamp.AgentConfigMap.Builder builder = Opamp.AgentConfigMap.newBuilder();
    builder.putConfigMap(
        configFileName,
        Opamp.AgentConfigFile.newBuilder().setBody(ByteString.copyFromUtf8(content)).build());
    return builder.build();
  }

  private static class TestCallback implements OpampClient.Callback {

    @Override
    public void onConnect(OpampClient client) {}

    @Override
    public void onConnectFailed(OpampClient client, Throwable throwable) {}

    @Override
    public void onErrorResponse(OpampClient client, Opamp.ServerErrorResponse errorResponse) {}

    @Override
    public void onMessage(OpampClient client, MessageData messageData) {}
  }
}
