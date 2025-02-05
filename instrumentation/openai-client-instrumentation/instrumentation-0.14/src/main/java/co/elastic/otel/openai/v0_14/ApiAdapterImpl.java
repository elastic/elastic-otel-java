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
package co.elastic.otel.openai.v0_14;

import co.elastic.otel.openai.wrappers.ApiAdapter;
import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionContentPart;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;

public class ApiAdapterImpl extends ApiAdapter {

  public static void init() {
    ApiAdapter.init(ApiAdapterImpl::new);
  }

  @Override
  public Object extractConcreteCompletionMessageParam(ChatCompletionMessageParam base) {
    if (base.isSystem()) {
      return base.asSystem();
    }
    if (base.isUser()) {
      return base.asUser();
    }
    if (base.isAssistant()) {
      return base.asAssistant();
    }
    if (base.isTool()) {
      return base.asTool();
    }
    throw new IllegalStateException("Unhandled message param type: " + base);
  }

  @Override
  public String asText(ChatCompletionToolMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String asText(ChatCompletionAssistantMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String asText(ChatCompletionSystemMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String asText(ChatCompletionUserMessageParam.Content content) {
    return content.isText() ? content.asText() : null;
  }

  @Override
  public String extractTextOrRefusal(
      ChatCompletionAssistantMessageParam.Content.ChatCompletionRequestAssistantMessageContentPart
          part) {
    if (part.isText()) {
      return part.asText().text();
    }
    if (part.isRefusal()) {
      return part.asRefusal().refusal();
    }
    return null;
  }

  @Override
  public String extractText(ChatCompletionContentPart part) {
    return part.isText() ? part.asText().text() : null;
  }

  @Override
  public String extractType(ChatCompletionCreateParams.ResponseFormat val) {
    if (val.isText()) {
      return val.asText()._type().toString();
    }
    if (val.isJsonObject()) {
      return val.asJsonObject()._type().toString();
    }
    if (val.isJsonSchema()) {
      return val.asJsonSchema()._type().toString();
    }
    return "";
  }
}
