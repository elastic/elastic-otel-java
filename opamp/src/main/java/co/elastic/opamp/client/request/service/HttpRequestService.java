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

import co.elastic.opamp.client.connectivity.http.HttpErrorException;
import co.elastic.opamp.client.connectivity.http.HttpSender;
import co.elastic.opamp.client.internal.periodictask.PeriodicTaskExecutor;
import co.elastic.opamp.client.request.Request;
import co.elastic.opamp.client.request.delay.AcceptsDelaySuggestion;
import co.elastic.opamp.client.request.delay.PeriodicDelay;
import co.elastic.opamp.client.response.Response;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import opamp.proto.Opamp;

public final class HttpRequestService implements RequestService, Runnable {
  private final HttpSender requestSender;
  private final PeriodicTaskExecutor executor;
  private final PeriodicDelay periodicRequestDelay;
  private final PeriodicDelay periodicRetryDelay;
  private final Object runningLock = new Object();
  private final AtomicBoolean retryModeEnabled = new AtomicBoolean(false);
  private Callback callback;
  private Supplier<Request> requestSupplier;
  private boolean isRunning = false;
  private boolean isStopped = false;
  public static final PeriodicDelay DEFAULT_DELAY_BETWEEN_REQUESTS =
      PeriodicDelay.ofFixedDuration(Duration.ofSeconds(30));

  /**
   * Creates an {@link HttpRequestService}.
   *
   * @param requestSender The HTTP sender implementation.
   */
  public static HttpRequestService create(HttpSender requestSender) {
    return create(requestSender, DEFAULT_DELAY_BETWEEN_REQUESTS, DEFAULT_DELAY_BETWEEN_REQUESTS);
  }

  /**
   * Creates an {@link HttpRequestService}.
   *
   * @param requestSender The HTTP sender implementation.
   * @param periodicRequestDelay The time to wait between requests in general.
   * @param periodicRetryDelay The time to wait between retries.
   */
  public static HttpRequestService create(
      HttpSender requestSender,
      PeriodicDelay periodicRequestDelay,
      PeriodicDelay periodicRetryDelay) {
    return new HttpRequestService(
        requestSender,
        PeriodicTaskExecutor.create(periodicRequestDelay),
        periodicRequestDelay,
        periodicRetryDelay);
  }

  HttpRequestService(
      HttpSender requestSender,
      PeriodicTaskExecutor executor,
      PeriodicDelay periodicRequestDelay,
      PeriodicDelay periodicRetryDelay) {
    this.requestSender = requestSender;
    this.executor = executor;
    this.periodicRequestDelay = periodicRequestDelay;
    this.periodicRetryDelay = periodicRetryDelay;
  }

  @Override
  public void start(Callback callback, Supplier<Request> requestSupplier) {
    synchronized (runningLock) {
      if (isStopped) {
        throw new IllegalStateException("RequestDispatcher has been stopped");
      }
      if (isRunning) {
        throw new IllegalStateException("RequestDispatcher is already running");
      }
      this.callback = callback;
      this.requestSupplier = requestSupplier;
      executor.start(this);
      isRunning = true;
    }
  }

  @Override
  public void stop() {
    synchronized (runningLock) {
      if (!isRunning || isStopped) {
        return;
      }
      isStopped = true;
      executor.executeNow();
      executor.stop();
    }
  }

  private void enableRetryMode(Duration suggestedDelay) {
    if (retryModeEnabled.compareAndSet(false, true)) {
      if (suggestedDelay != null && periodicRetryDelay instanceof AcceptsDelaySuggestion) {
        ((AcceptsDelaySuggestion) periodicRetryDelay).suggestDelay(suggestedDelay);
      }
      executor.setPeriodicDelay(periodicRetryDelay);
    }
  }

  private void disableRetryMode() {
    if (retryModeEnabled.compareAndSet(true, false)) {
      executor.setPeriodicDelay(periodicRequestDelay);
    }
  }

  @Override
  public void sendRequest() {
    if (!retryModeEnabled.get()) {
      executor.executeNow();
    }
  }

  @Override
  public void run() {
    doSendRequest();
  }

  private void doSendRequest() {
    try {
      Opamp.AgentToServer agentToServer = requestSupplier.get().getAgentToServer();

      try (HttpSender.Response response =
          requestSender
              .send(
                  new ByteArrayWriter(agentToServer.toByteArray()),
                  agentToServer.getSerializedSize())
              .get()) {
        if (isSuccessful(response)) {
          handleResponse(
              Response.create(Opamp.ServerToAgent.parseFrom(response.bodyInputStream())));
        } else {
          handleHttpError(response);
        }
      } catch (IOException e) {
        callback.onRequestFailed(e);
      }

    } catch (InterruptedException e) {
      callback.onRequestFailed(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof UnknownHostException || cause instanceof ConnectException) {
        callback.onConnectionFailed(cause);
      } else {
        callback.onRequestFailed(cause);
      }
    }
  }

  private void handleHttpError(HttpSender.Response response) {
    int errorCode = response.statusCode();
    callback.onRequestFailed(new HttpErrorException(errorCode, response.statusMessage()));

    if (errorCode == 503 || errorCode == 429) {
      String retryAfterHeader = response.getHeader("Retry-After");
      Duration retryAfter = null;
      if (retryAfterHeader != null) {
        // retryAfter = TODO parse header to duration
      }
      enableRetryMode(retryAfter);
    }
  }

  private boolean isSuccessful(HttpSender.Response response) {
    return response.statusCode() >= 200 && response.statusCode() < 300;
  }

  private void handleResponse(Response response) {
    if (retryModeEnabled.get()) {
      disableRetryMode();
    }
    Opamp.ServerToAgent serverToAgent = response.getServerToAgent();

    if (serverToAgent.hasErrorResponse()) {
      handleErrorResponse(serverToAgent.getErrorResponse());
    }

    callback.onRequestSuccess(response);
  }

  private void handleErrorResponse(Opamp.ServerErrorResponse errorResponse) {
    if (errorResponse
        .getType()
        .equals(Opamp.ServerErrorResponseType.ServerErrorResponseType_Unavailable)) {
      Duration retryAfter = null;
      if (errorResponse.hasRetryInfo()) {
        retryAfter = Duration.ofNanos(errorResponse.getRetryInfo().getRetryAfterNanoseconds());
      }
      enableRetryMode(retryAfter);
    }
  }

  private static class ByteArrayWriter implements Consumer<OutputStream> {
    private final byte[] data;

    private ByteArrayWriter(byte[] data) {
      this.data = data;
    }

    @Override
    public void accept(OutputStream outputStream) {
      try {
        outputStream.write(data);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
