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

import co.elastic.opamp.client.OpampClient;
import co.elastic.opamp.client.internal.request.fields.FieldStateChangeListener;
import co.elastic.opamp.client.internal.request.fields.FieldStateObserver;
import co.elastic.opamp.client.internal.request.fields.FieldType;
import co.elastic.opamp.client.internal.request.fields.appenders.AgentDescriptionAppender;
import co.elastic.opamp.client.internal.request.fields.appenders.AgentDisconnectAppender;
import co.elastic.opamp.client.internal.request.fields.appenders.AgentToServerAppenders;
import co.elastic.opamp.client.internal.request.fields.appenders.CapabilitiesAppender;
import co.elastic.opamp.client.internal.request.fields.appenders.EffectiveConfigAppender;
import co.elastic.opamp.client.internal.request.fields.appenders.FlagsAppender;
import co.elastic.opamp.client.internal.request.fields.appenders.InstanceUidAppender;
import co.elastic.opamp.client.internal.request.fields.appenders.RemoteConfigStatusAppender;
import co.elastic.opamp.client.internal.request.fields.appenders.SequenceNumberAppender;
import co.elastic.opamp.client.internal.request.fields.recipe.RecipeManager;
import co.elastic.opamp.client.internal.request.fields.recipe.RequestRecipe;
import co.elastic.opamp.client.internal.state.OpampClientState;
import co.elastic.opamp.client.request.Request;
import co.elastic.opamp.client.request.service.RequestService;
import co.elastic.opamp.client.response.MessageData;
import co.elastic.opamp.client.response.Response;
import co.elastic.opamp.client.state.observer.Observable;
import com.google.protobuf.ByteString;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import opamp.proto.Opamp;

public final class OpampClientImpl
    implements OpampClient, FieldStateChangeListener, RequestService.Callback, Supplier<Request> {
  private final RequestService requestService;
  private final AgentToServerAppenders appenders;
  private final OpampClientState state;
  private final RecipeManager recipeManager;
  private final Lock runningLock = new ReentrantLock();
  private Callback callback;
  private boolean isRunning;
  private boolean isStopped;

  /** Fields that must always be sent. */
  private static final List<FieldType> CONSTANT_FIELDS =
      Arrays.asList(FieldType.INSTANCE_UID, FieldType.SEQUENCE_NUM, FieldType.CAPABILITIES);

  /**
   * Fields that should only be sent in the first message and then omitted in following messages,
   * unless their value changes or the server requests a full message.
   */
  private static final List<FieldType> COMPRESSABLE_FIELDS =
      Arrays.asList(
          FieldType.AGENT_DESCRIPTION, FieldType.EFFECTIVE_CONFIG, FieldType.REMOTE_CONFIG_STATUS);

  public static OpampClientImpl create(RequestService requestService, OpampClientState state) {
    AgentToServerAppenders appenders =
        new AgentToServerAppenders(
            AgentDescriptionAppender.create(state.agentDescriptionState),
            EffectiveConfigAppender.create(state.effectiveConfigState),
            RemoteConfigStatusAppender.create(state.remoteConfigStatusState),
            SequenceNumberAppender.create(state.sequenceNumberState),
            CapabilitiesAppender.create(state.capabilitiesState),
            InstanceUidAppender.create(state.instanceUidState),
            FlagsAppender.create(),
            AgentDisconnectAppender.create());
    RecipeManager recipeManager = new RecipeManager();
    recipeManager.setConstantFields(CONSTANT_FIELDS);
    return new OpampClientImpl(requestService, appenders, state, recipeManager);
  }

  private OpampClientImpl(
      RequestService requestService,
      AgentToServerAppenders appenders,
      OpampClientState state,
      RecipeManager recipeManager) {
    this.requestService = requestService;
    this.appenders = appenders;
    this.state = state;
    this.recipeManager = recipeManager;
  }

  @Override
  public void start(Callback callback) {
    runningLock.lock();
    try {
      if (!isRunning) {
        isRunning = true;
        this.callback = callback;
        observeStateChange();
        disableCompression();
        requestService.start(this, this);
        requestService.sendRequest();
      } else {
        throw new IllegalStateException("The client has already been started");
      }
    } finally {
      runningLock.unlock();
    }
  }

  @Override
  public void stop() {
    runningLock.lock();
    try {
      if (!isRunning) {
        throw new IllegalStateException("The client has not been started");
      }
      if (!isStopped) {
        isStopped = true;
        prepareDisconnectRequest();
        requestService.stop();
      } else {
        throw new IllegalStateException("The client has already been stopped");
      }
    } finally {
      runningLock.unlock();
    }
  }

  @Override
  public void setRemoteConfigStatus(Opamp.RemoteConfigStatus remoteConfigStatus) {
    state.remoteConfigStatusState.set(remoteConfigStatus);
  }

  @Override
  public void onConnectionSuccess() {
    callback.onConnect(this);
  }

  @Override
  public void onConnectionFailed(Throwable throwable, Duration nextTry) {
    callback.onConnectFailed(this, throwable, nextTry);
    preserveFailedRequestRecipe();
  }

  @Override
  public void onRequestSuccess(Response response) {
    state.sequenceNumberState.increment();
    if (response == null) return;

    handleResponsePayload(response.getServerToAgent());
  }

  @Override
  public void onRequestFailed(Throwable throwable, Duration nextTry) {
    final Opamp.ServerErrorResponse error;
    if (throwable == null) {
      error =
          Opamp.ServerErrorResponse.newBuilder()
              .setErrorMessageBytes(ByteString.copyFromUtf8("null"))
              .build();
    } else {
      error =
          Opamp.ServerErrorResponse.newBuilder()
              .setErrorMessageBytes(
                  ByteString.copyFromUtf8(
                      throwable.getClass().getName() + ": " + throwable.getMessage()))
              .build();
    }
    callback.onErrorResponse(this, error, nextTry);
    preserveFailedRequestRecipe();
  }

  private void handleResponsePayload(Opamp.ServerToAgent response) {
    if (response.hasErrorResponse()) {
      Opamp.ServerErrorResponse errorResponse = response.getErrorResponse();
      callback.onErrorResponse(this, errorResponse, null);
    }
    long reportFullState = Opamp.ServerToAgentFlags.ServerToAgentFlags_ReportFullState_VALUE;
    if ((response.getFlags() & reportFullState) == reportFullState) {
      disableCompression();
    }
    handleAgentIdentification(response);

    boolean notifyOnMessage = false;
    MessageData.Builder messageBuilder = MessageData.builder();

    if (response.hasRemoteConfig()) {
      notifyOnMessage = true;
      messageBuilder.setRemoteConfig(response.getRemoteConfig());
    }

    if (notifyOnMessage) {
      callback.onMessage(this, messageBuilder.build());
    }
  }

  private void handleAgentIdentification(Opamp.ServerToAgent response) {
    if (response.hasAgentIdentification()) {
      ByteString newInstanceUid = response.getAgentIdentification().getNewInstanceUid();
      if (!newInstanceUid.isEmpty()) {
        state.instanceUidState.set(newInstanceUid.toByteArray());
      }
    }
  }

  private void disableCompression() {
    recipeManager.next().addAllFields(COMPRESSABLE_FIELDS);
  }

  private void prepareDisconnectRequest() {
    recipeManager.next().addField(FieldType.AGENT_DISCONNECT);
  }

  private void preserveFailedRequestRecipe() {
    final RequestRecipe previous = recipeManager.previous();
    if (previous != null) {
      recipeManager.next().merge(previous);
    }
  }

  @Override
  public void onStateForFieldChanged(FieldType fieldType) {
    recipeManager.next().addField(fieldType);
    requestService.sendRequest();
  }

  @Override
  public Request get() {
    Opamp.AgentToServer.Builder builder = Opamp.AgentToServer.newBuilder();
    for (FieldType field : recipeManager.next().build().getFields()) {
      appenders.getForField(field).appendTo(builder);
    }
    return Request.create(builder.build());
  }

  private void observeStateChange() {
    addStateForFieldObserver(state.agentDescriptionState, FieldType.AGENT_DESCRIPTION);
    addStateForFieldObserver(state.effectiveConfigState, FieldType.EFFECTIVE_CONFIG);
    addStateForFieldObserver(state.remoteConfigStatusState, FieldType.REMOTE_CONFIG_STATUS);
    addStateForFieldObserver(state.capabilitiesState, FieldType.CAPABILITIES);
    addStateForFieldObserver(state.instanceUidState, FieldType.INSTANCE_UID);
  }

  private void addStateForFieldObserver(Observable observable, FieldType fieldType) {
    observable.addObserver(new FieldStateObserver(this, fieldType));
  }
}
