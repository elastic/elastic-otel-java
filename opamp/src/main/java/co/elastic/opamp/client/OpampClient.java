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

import co.elastic.opamp.client.response.MessageData;
import java.time.Duration;
import opamp.proto.Opamp;

public interface OpampClient {

  static OpampClientBuilder builder() {
    return new OpampClientBuilder();
  }

  /**
   * Starts the client and begin attempts to connect to the Server. Once connection is established
   * the client will attempt to maintain it by reconnecting if the connection is lost. All failed
   * connection attempts will be reported via {@link Callback#onConnectFailed(OpampClient,
   * Throwable, Duration)} callback.
   *
   * <p>This method does not wait until the connection to the Server is established and will likely
   * return before the connection attempts are even made.
   *
   * <p>This method may be called only once.
   *
   * @param callback The Callback to which the Client will notify about any Server requests and
   *     responses.
   */
  void start(Callback callback);

  /**
   * Stops the client. May be called only after {@link
   * #start(co.elastic.opamp.client.OpampClient.Callback)}. May be called only once. After this call
   * returns successfully it is guaranteed that no callbacks will be called. Once stopped, the
   * client cannot be started again.
   */
  void stop();

  /**
   * Sets the current remote config status which will be sent in the next agent to server request.
   *
   * @param remoteConfigStatus The new remote config status.
   */
  void setRemoteConfigStatus(Opamp.RemoteConfigStatus remoteConfigStatus);

  interface Callback {
    /**
     * Called when the connection is successfully established to the Server. May be called after
     * {@link #start(co.elastic.opamp.client.OpampClient.Callback)} is called and every time a
     * connection is established to the Server. For WebSocket clients this is called after the
     * handshake is completed without any error. For HTTP clients this is called for any request if
     * the response status is OK.
     *
     * @param client The relevant {@link co.elastic.opamp.client.OpampClient} instance.
     */
    void onConnect(OpampClient client);

    /**
     * Called when the connection to the Server cannot be established. May be called after {@link
     * #start(co.elastic.opamp.client.OpampClient.Callback)} is called and tries to connect to the
     * Server. May also be called if the connection is lost and reconnection attempt fails.
     *
     * @param client The relevant {@link co.elastic.opamp.client.OpampClient} instance.
     * @param throwable The exception.
     */
    void onConnectFailed(OpampClient client, Throwable throwable, Duration nextTry);

    /**
     * Called when the Server reports an error in response to some previously sent request. Useful
     * for logging purposes. The Agent should not attempt to process the error by reconnecting or
     * retrying previous operations. The client handles the ErrorResponse_UNAVAILABLE case
     * internally by performing retries as necessary.
     *
     * @param client The relevant {@link co.elastic.opamp.client.OpampClient} instance.
     * @param errorResponse The error returned by the Server.
     */
    void onErrorResponse(
        OpampClient client, Opamp.ServerErrorResponse errorResponse, Duration nextTry);

    /**
     * Called when the Agent receives a message that needs processing. See {@link
     * co.elastic.opamp.client.response.MessageData} definition for the data that may be available
     * for processing. During onMessage execution the {@link co.elastic.opamp.client.OpampClient}
     * functions that change the status of the client may be called, e.g. if RemoteConfig is
     * processed then {@link #setRemoteConfigStatus(opamp.proto.Opamp.RemoteConfigStatus)} should be
     * called to reflect the processing result. These functions may also be called after onMessage
     * returns. This is advisable if processing can take a long time. In that case returning quickly
     * is preferable to avoid blocking the {@link co.elastic.opamp.client.OpampClient}.
     *
     * @param client The relevant {@link co.elastic.opamp.client.OpampClient} instance.
     * @param messageData The server response data that needs processing.
     */
    void onMessage(OpampClient client, MessageData messageData);
  }
}
