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
package co.elastic.otel.openai.v1_1.wrappers;

import com.openai.client.OpenAIClient;
import io.opentelemetry.api.internal.ConfigUtil;
import java.lang.reflect.Method;
import java.net.URI;

public class InstrumentedOpenAiClient
    extends DelegatingInvocationHandler<OpenAIClient, InstrumentedOpenAiClient> {

  // Visible and non-final for testing
  InstrumentationSettings settings;

  private InstrumentedOpenAiClient(OpenAIClient delegate, InstrumentationSettings settings) {
    super(delegate);
    this.settings = settings;
  }

  @Override
  protected Class<OpenAIClient> getProxyType() {
    return OpenAIClient.class;
  }

  public static class Builder {

    private final OpenAIClient delegate;

    private boolean captureMessageContent =
        Boolean.valueOf(
            ConfigUtil.getString("otel.instrumentation.genai.capture.message.content", "false"));

    private boolean emitEvents =
        Boolean.valueOf(
            ConfigUtil.getString(
                "elastic.otel.java.instrumentation.genai.emit.events", "" + captureMessageContent));

    private String baseUrl;

    private Builder(OpenAIClient delegate) {
      this.delegate = delegate;
    }

    public Builder captureMessageContent(boolean captureMessageContent) {
      this.captureMessageContent = captureMessageContent;
      return this;
    }

    public Builder emitEvents(boolean emitEvents) {
      this.emitEvents = emitEvents;
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public OpenAIClient build() {
      if (delegate instanceof InstrumentedOpenAiClient) {
        return delegate;
      }
      String hostname = null;
      Long port = null;
      if (baseUrl != null) {
        try {
          URI parsed = new URI(baseUrl);
          hostname = parsed.getHost();
          int urlPort = parsed.getPort();
          if (urlPort != -1) {
            port = (long) urlPort;
          } else {
            long defaultPort = parsed.toURL().getDefaultPort();
            if (defaultPort != -1) {
              port = defaultPort;
            }
          }
        } catch (Exception e) {
          // ignore malformed
        }
      }
      return new InstrumentedOpenAiClient(
              delegate,
              new InstrumentationSettings(emitEvents, captureMessageContent, hostname, port))
          .createProxy();
    }
  }

  public static Builder wrap(OpenAIClient delegate) {
    return new Builder(delegate);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    Class<?>[] parameterTypes = method.getParameterTypes();
    if (methodName.equals("chat") && parameterTypes.length == 0) {
      return new InstrumentedChatService(delegate.chat(), settings).createProxy();
    }
    if (methodName.equals("embeddings") && parameterTypes.length == 0) {
      return new InstrumentedEmbeddingsService(delegate.embeddings(), settings).createProxy();
    }
    return super.invoke(proxy, method, args);
  }
}
