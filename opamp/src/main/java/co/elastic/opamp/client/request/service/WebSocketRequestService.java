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
package co.elastic.opamp.client.request.service;

import co.elastic.opamp.client.connectivity.websocket.WebSocket;
import co.elastic.opamp.client.connectivity.websocket.WebSocketListener;
import co.elastic.opamp.client.internal.periodictask.PeriodicTaskExecutor;
import co.elastic.opamp.client.request.Request;
import co.elastic.opamp.client.request.delay.AcceptsDelaySuggestion;
import co.elastic.opamp.client.request.delay.PeriodicDelay;
import co.elastic.opamp.client.response.Response;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import opamp.proto.Opamp;

public final class WebSocketRequestService implements RequestService, WebSocketListener, Runnable {
  private final WebSocket webSocket;
  private final PeriodicDelay periodicRetryDelay;
  private final AtomicBoolean retryConnectionModeEnabled = new AtomicBoolean(false);
  private final AtomicBoolean websocketRunning = new AtomicBoolean(false);
  private final AtomicBoolean hasPendingRequest = new AtomicBoolean(false);
  private PeriodicTaskExecutor executor;
  private Callback callback;
  private Supplier<Request> requestSupplier;
  public static final PeriodicDelay DEFAULT_DELAY_BETWEEN_RETRIES =
      PeriodicDelay.ofFixedDuration(Duration.ofSeconds(30));

  /**
   * Creates an {@link WebSocketRequestService}.
   *
   * @param webSocket The WebSocket implementation.
   */
  public static WebSocketRequestService create(WebSocket webSocket) {
    return new WebSocketRequestService(webSocket, DEFAULT_DELAY_BETWEEN_RETRIES);
  }

  /**
   * Creates an {@link WebSocketRequestService}.
   *
   * @param webSocket The WebSocket implementation.
   * @param periodicRetryDelay The time to wait between retries.
   */
  public static WebSocketRequestService create(
      WebSocket webSocket, PeriodicDelay periodicRetryDelay) {
    return new WebSocketRequestService(webSocket, periodicRetryDelay);
  }

  WebSocketRequestService(WebSocket webSocket, PeriodicDelay periodicRetryDelay) {
    this.webSocket = webSocket;
    this.periodicRetryDelay = periodicRetryDelay;
  }

  @Override
  public void start(Callback callback, Supplier<Request> requestSupplier) {
    this.callback = callback;
    this.requestSupplier = requestSupplier;
    if (!websocketRunning.get()) {
      webSocket.start(this);
    }
  }

  @Override
  public void sendRequest() {
    if (websocketRunning.get()) {
      doSendRequest();
    } else {
      hasPendingRequest.set(true);
    }
  }

  private void doSendRequest() {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      CodedOutputStream codedOutput = CodedOutputStream.newInstance(outputStream);
      codedOutput.writeUInt64NoTag(0);
      requestSupplier.get().getAgentToServer().writeTo(codedOutput);
      codedOutput.flush();
      webSocket.send(outputStream.toByteArray());
    } catch (IOException e) {
      callback.onRequestFailed(e, null);
    }
  }

  @Override
  public void stop() {
    sendRequest();
    stopWebSocket();
  }

  @Override
  public void onOpened(WebSocket webSocket) {
    disableRetryMode();
    websocketRunning.set(true);
    callback.onConnectionSuccess();
    if (hasPendingRequest.compareAndSet(true, false)) {
      sendRequest();
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, byte[] data) {
    try {
      Opamp.ServerToAgent serverToAgent = readServerToAgent(data);

      if (serverToAgent.hasErrorResponse()) {
        handleServerError(serverToAgent.getErrorResponse());
      }

      callback.onRequestSuccess(Response.create(serverToAgent));
    } catch (IOException e) {
      callback.onRequestFailed(e, null);
    }
  }

  private static Opamp.ServerToAgent readServerToAgent(byte[] data) throws IOException {
    CodedInputStream codedInputStream = CodedInputStream.newInstance(data);
    long header = codedInputStream.readRawVarint64();
    int totalBytesRead = codedInputStream.getTotalBytesRead();
    int payloadSize = data.length - totalBytesRead;
    byte[] payload = new byte[payloadSize];
    System.arraycopy(data, totalBytesRead, payload, 0, payloadSize);
    return Opamp.ServerToAgent.parseFrom(payload);
  }

  private void handleServerError(Opamp.ServerErrorResponse errorResponse) {
    if (shouldRetry(errorResponse)) {
      Duration retryAfter = null;

      if (errorResponse.hasRetryInfo()) {
        retryAfter = Duration.ofNanos(errorResponse.getRetryInfo().getRetryAfterNanoseconds());
      }

      enableRetryMode(retryAfter);
    }
  }

  private static boolean shouldRetry(Opamp.ServerErrorResponse errorResponse) {
    return errorResponse
        .getType()
        .equals(Opamp.ServerErrorResponseType.ServerErrorResponseType_Unavailable);
  }

  private void enableRetryMode(Duration retryAfter) {
    if (retryConnectionModeEnabled.compareAndSet(false, true)) {
      stopWebSocket();
      if (retryAfter != null && periodicRetryDelay instanceof AcceptsDelaySuggestion) {
        ((AcceptsDelaySuggestion) periodicRetryDelay).suggestDelay(retryAfter);
      }
      executor = PeriodicTaskExecutor.create(periodicRetryDelay);
      executor.start(this);
    }
  }

  private void disableRetryMode() {
    if (retryConnectionModeEnabled.compareAndSet(true, false)) {
      executor.stop();
      executor = null;
    }
  }

  @Override
  public void onClosed(WebSocket webSocket) {
    websocketRunning.set(false);
  }

  @Override
  public void onFailure(WebSocket webSocket, Throwable t) {
    callback.onConnectionFailed(t, null);
    enableRetryMode(null);
  }

  @Override
  public void run() {
    retry();
  }

  private void retry() {
    if (retryConnectionModeEnabled.get()) {
      hasPendingRequest.set(true);
      webSocket.start(this);
    }
  }

  private void stopWebSocket() {
    if (websocketRunning.get()) {
      webSocket.stop();
    }
  }
}
