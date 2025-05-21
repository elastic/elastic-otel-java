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
package co.elastic.opamp.client.connectivity.websocket;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OkHttpWebSocket extends okhttp3.WebSocketListener implements WebSocket {
  private final OkHttpClient client;
  private final String url;
  private WebSocketListener listener;
  private okhttp3.WebSocket webSocket;

  public static OkHttpWebSocket create(String url) {
    OkHttpClient client = new OkHttpClient();
    return new OkHttpWebSocket(client, url);
  }

  public OkHttpWebSocket(OkHttpClient client, String url) {
    this.client = client;
    this.url = url;
  }

  @Override
  public void start(WebSocketListener listener) {
    this.listener = listener;
    okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
    webSocket = client.newWebSocket(request, this);
  }

  @Override
  public void send(byte[] request) {
    webSocket.send(ByteString.of(request));
  }

  @Override
  public void stop() {
    webSocket.cancel();
  }

  @Override
  public void onClosed(@NotNull okhttp3.WebSocket webSocket, int code, @NotNull String reason) {
    listener.onClosed(this);
  }

  @Override
  public void onFailure(
      @NotNull okhttp3.WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
    listener.onFailure(this, t);
  }

  @Override
  public void onMessage(@NotNull okhttp3.WebSocket webSocket, @NotNull ByteString bytes) {
    listener.onMessage(this, bytes.toByteArray());
  }

  @Override
  public void onOpen(@NotNull okhttp3.WebSocket webSocket, @NotNull Response response) {
    listener.onOpened(this);
  }
}
