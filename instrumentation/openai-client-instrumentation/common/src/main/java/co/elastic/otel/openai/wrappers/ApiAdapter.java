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
package co.elastic.otel.openai.wrappers;

import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionContentPart;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import java.util.function.Supplier;

/**
 * Api Adapter to encapsulate breaking changes across openai-client versions. If e.g. methods are
 * renamed we add a adapter method here, so that we can provide per-version implementations. These
 * implementations have to be added to instrumentations as helpers, which also ensures muzzle works
 * effectively.
 */
public abstract class ApiAdapter {

  private static volatile ApiAdapter instance;

  public static ApiAdapter get() {
    return instance;
  }

  protected static void init(Supplier<ApiAdapter> implementation) {
    if (instance == null) {
      synchronized (ApiAdapter.class) {
        if (instance == null) {
          instance = implementation.get();
        }
      }
    }
  }

  /**
   * Extracts the concrete message object e.g. ({@link ChatCompletionUserMessageParam}) from the
   * given encapsulating {@link ChatCompletionMessageParam}.
   *
   * @param base the encapsulating param
   * @return the unboxed concrete message param type
   */
  public abstract Object extractConcreteCompletionMessageParam(ChatCompletionMessageParam base);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionToolMessageParam.Content content);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionAssistantMessageParam.Content content);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionSystemMessageParam.Content content);

  /**
   * @return the contained text, if the content is next. null otherwise.
   */
  public abstract String asText(ChatCompletionUserMessageParam.Content content);

  /**
   * @return the text or refusal reason if either is available, otherwise null
   */
  public abstract String extractTextOrRefusal(
      ChatCompletionAssistantMessageParam.Content.ChatCompletionRequestAssistantMessageContentPart
          part);

  /**
   * @return the text if available, otherwise null
   */
  public abstract String extractText(ChatCompletionContentPart part);

  public abstract String extractType(ChatCompletionCreateParams.ResponseFormat val);
}
