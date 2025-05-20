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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import co.elastic.opamp.client.connectivity.http.HttpErrorException;
import co.elastic.opamp.client.connectivity.http.HttpSender;
import co.elastic.opamp.client.internal.periodictask.PeriodicTaskExecutor;
import co.elastic.opamp.client.request.Request;
import co.elastic.opamp.client.request.delay.AcceptsDelaySuggestion;
import co.elastic.opamp.client.request.delay.PeriodicDelay;
import co.elastic.opamp.client.response.Response;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import opamp.proto.Opamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpRequestServiceTest {
  @Mock private HttpSender requestSender;
  @Mock private PeriodicDelay periodicRequestDelay;
  @Mock private TestPeriodicRetryDelay periodicRetryDelay;
  @Mock private PeriodicTaskExecutor executor;
  @Mock private RequestService.Callback callback;
  @Mock private Supplier<Request> requestSupplier;
  @Mock private Request request;
  private static final int REQUEST_SIZE = 100;
  private HttpRequestService httpRequestService;

  @BeforeEach
  void setUp() {
    httpRequestService =
        new HttpRequestService(requestSender, executor, periodicRequestDelay, periodicRetryDelay);
  }

  @Test
  void verifyStart() {
    httpRequestService.start(callback, requestSupplier);

    InOrder inOrder = inOrder(periodicRequestDelay, executor);
    inOrder.verify(executor).start(httpRequestService);

    // Try starting it again:
    try {
      httpRequestService.start(callback, requestSupplier);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("RequestDispatcher is already running");
    }
  }

  @Test
  void verifyStop() {
    httpRequestService.start(callback, requestSupplier);
    httpRequestService.stop();

    verify(executor).stop();

    // Try stopping it again:
    clearInvocations(executor);
    httpRequestService.stop();
    verifyNoInteractions(executor);
  }

  @Test
  void verifyStop_whenNotStarted() {
    httpRequestService.stop();

    verifyNoInteractions(executor, requestSender, periodicRequestDelay);
  }

  @Test
  void whenTryingToStartAfterStopHasBeenCalled_throwException() {
    httpRequestService.start(callback, requestSupplier);
    httpRequestService.stop();
    try {
      httpRequestService.start(callback, requestSupplier);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("RequestDispatcher has been stopped");
    }
  }

  @Test
  void verifySendingRequest_happyPath() {
    HttpSender.Response httpResponse = mock();
    Opamp.ServerToAgent serverToAgent = Opamp.ServerToAgent.getDefaultInstance();
    attachServerToAgentMessage(serverToAgent.toByteArray(), httpResponse);
    prepareRequest();
    enqueueResponse(httpResponse);

    httpRequestService.run();

    verify(requestSender).send(any(), eq(REQUEST_SIZE));
    verify(callback).onRequestSuccess(Response.create(serverToAgent));
  }

  @Test
  void verifySendingRequest_whenTheresAParsingError() {
    HttpSender.Response httpResponse = mock();
    attachServerToAgentMessage(new byte[] {1, 2, 3}, httpResponse);
    prepareRequest();
    enqueueResponse(httpResponse);

    httpRequestService.run();

    verify(requestSender).send(any(), eq(REQUEST_SIZE));
    verify(callback).onRequestFailed(any());
  }

  @Test
  void verifySendingRequest_whenThereIsAnExecutionError()
      throws ExecutionException, InterruptedException {
    prepareRequest();
    CompletableFuture<HttpSender.Response> future = mock();
    doReturn(future).when(requestSender).send(any(), anyInt());
    Exception myException = mock();
    doThrow(new ExecutionException(myException)).when(future).get();

    httpRequestService.run();

    verify(requestSender).send(any(), eq(REQUEST_SIZE));
    verify(callback).onRequestFailed(myException);
  }

  @Test
  void verifySendingRequest_whenThereIsAnInterruptedException()
      throws ExecutionException, InterruptedException {
    prepareRequest();
    CompletableFuture<HttpSender.Response> future = mock();
    doReturn(future).when(requestSender).send(any(), anyInt());
    InterruptedException myException = mock();
    doThrow(myException).when(future).get();

    httpRequestService.run();

    verify(requestSender).send(any(), eq(REQUEST_SIZE));
    verify(callback).onRequestFailed(myException);
  }

  @Test
  void verifySendingRequest_whenThereIsAGenericHttpError() {
    HttpSender.Response response = mock();
    doReturn(500).when(response).statusCode();
    doReturn("Error message").when(response).statusMessage();
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestFailed(new HttpErrorException(500, "Error message"));
    verifyNoInteractions(executor);
  }

  @Test
  void verifySendingRequest_whenThereIsATooManyRequestsError() {
    HttpSender.Response response = mock();
    doReturn(429).when(response).statusCode();
    doReturn("Error message").when(response).statusMessage();
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestFailed(new HttpErrorException(429, "Error message"));
    verify(executor).setPeriodicDelay(periodicRetryDelay);
  }

  @Test
  void verifySendingRequest_whenServerProvidesRetryInfo_usingTheProvidedInfo() {
    HttpSender.Response response = mock();
    long nanosecondsToWaitForRetry = 1000;
    Opamp.ServerErrorResponse errorResponse =
        Opamp.ServerErrorResponse.newBuilder()
            .setType(Opamp.ServerErrorResponseType.ServerErrorResponseType_Unavailable)
            .setRetryInfo(
                Opamp.RetryInfo.newBuilder()
                    .setRetryAfterNanoseconds(nanosecondsToWaitForRetry)
                    .build())
            .build();
    Opamp.ServerToAgent serverToAgent =
        Opamp.ServerToAgent.newBuilder().setErrorResponse(errorResponse).build();
    attachServerToAgentMessage(serverToAgent.toByteArray(), response);
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestSuccess(Response.create(serverToAgent));
    verify(periodicRetryDelay).suggestDelay(Duration.ofNanos(nanosecondsToWaitForRetry));
    verify(executor).setPeriodicDelay(periodicRetryDelay);
  }

  @Test
  void verifySendingRequest_whenServerIsUnavailable() {
    HttpSender.Response response = mock();
    Opamp.ServerErrorResponse errorResponse =
        Opamp.ServerErrorResponse.newBuilder()
            .setType(Opamp.ServerErrorResponseType.ServerErrorResponseType_Unavailable)
            .build();
    Opamp.ServerToAgent serverToAgent =
        Opamp.ServerToAgent.newBuilder().setErrorResponse(errorResponse).build();
    attachServerToAgentMessage(serverToAgent.toByteArray(), response);
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestSuccess(Response.create(serverToAgent));
    verify(periodicRetryDelay, never()).suggestDelay(any());
    verify(executor).setPeriodicDelay(periodicRetryDelay);
  }

  @Test
  void verifySendingRequest_whenThereIsAServiceUnavailableError() {
    HttpSender.Response response = mock();
    doReturn(503).when(response).statusCode();
    doReturn("Error message").when(response).statusMessage();
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();

    verify(callback).onRequestFailed(new HttpErrorException(503, "Error message"));
    verify(executor).setPeriodicDelay(periodicRetryDelay);
  }

  @Test
  void verifySendingRequest_duringRegularMode() {
    httpRequestService.sendRequest();

    verify(executor).executeNow();
  }

  @Test
  void verifySendingRequest_duringRetryMode() {
    enableRetryMode();

    httpRequestService.sendRequest();

    verify(executor, never()).executeNow();
  }

  @Test
  void verifySuccessfulSendingRequest_duringRetryMode() {
    enableRetryMode();
    HttpSender.Response response = mock();
    doReturn(200).when(response).statusCode();
    enqueueResponse(response);

    httpRequestService.run();

    verify(executor).setPeriodicDelay(periodicRequestDelay);
  }

  private void enableRetryMode() {
    HttpSender.Response response = mock();
    doReturn(503).when(response).statusCode();
    doReturn("Error message").when(response).statusMessage();
    prepareRequest();
    enqueueResponse(response);

    httpRequestService.run();
  }

  private void prepareRequest() {
    httpRequestService.start(callback, requestSupplier);
    clearInvocations(executor);
    Opamp.AgentToServer agentToServer = mock(Opamp.AgentToServer.class);
    doReturn(REQUEST_SIZE).when(agentToServer).getSerializedSize();
    doReturn(agentToServer).when(request).getAgentToServer();
    doReturn(request).when(requestSupplier).get();
  }

  private void enqueueResponse(HttpSender.Response httpResponse) {
    doReturn(CompletableFuture.completedFuture(httpResponse))
        .when(requestSender)
        .send(any(), anyInt());
  }

  private static void attachServerToAgentMessage(
      byte[] serverToAgent, HttpSender.Response httpResponse) {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serverToAgent);
    doReturn(200).when(httpResponse).statusCode();
    doReturn(byteArrayInputStream).when(httpResponse).bodyInputStream();
  }

  private static class TestPeriodicRetryDelay implements PeriodicDelay, AcceptsDelaySuggestion {

    @Override
    public void suggestDelay(Duration delay) {}

    @Override
    public Duration getNextDelay() {
      return null;
    }

    @Override
    public void reset() {}
  }
}
