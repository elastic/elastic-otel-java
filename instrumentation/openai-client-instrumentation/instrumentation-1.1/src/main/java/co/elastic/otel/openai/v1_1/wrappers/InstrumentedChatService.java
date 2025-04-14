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

import com.openai.services.blocking.ChatService;
import java.lang.reflect.Method;

public class InstrumentedChatService
    extends DelegatingInvocationHandler<ChatService, InstrumentedChatService> {

  private final InstrumentationSettings settings;

  public InstrumentedChatService(ChatService delegate, InstrumentationSettings settings) {
    super(delegate);
    this.settings = settings;
  }

  @Override
  protected Class<ChatService> getProxyType() {
    return ChatService.class;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    Class<?>[] parameterTypes = method.getParameterTypes();
    if (methodName.equals("completions") && parameterTypes.length == 0) {
      return new InstrumentedChatCompletionService(delegate.completions(), settings).createProxy();
    }
    return super.invoke(proxy, method, args);
  }
}
