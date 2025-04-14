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

import co.elastic.otel.openai.ChatTestBase;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.util.List;

class ChatTest extends ChatTestBase {

  @Override
  protected ChatCompletionMessageParam createAssistantMessage(String content) {
    return ChatCompletionMessageParam.ofAssistant(
        ChatCompletionAssistantMessageParam.builder()
            .content(ChatCompletionAssistantMessageParam.Content.ofText(content))
            .build());
  }

  @Override
  protected ChatCompletionMessageParam createAssistantMessage(
      List<ChatCompletionMessageToolCall> toolCalls) {
    return ChatCompletionMessageParam.ofAssistant(
        ChatCompletionAssistantMessageParam.builder()
            .content(ChatCompletionAssistantMessageParam.Content.ofText(""))
            .toolCalls(toolCalls)
            .build());
  }

  @Override
  protected ChatCompletionMessageParam createUserMessage(String content) {
    return ChatCompletionMessageParam.ofUser(
        ChatCompletionUserMessageParam.builder()
            .content(ChatCompletionUserMessageParam.Content.ofText(content))
            .build());
  }

  @Override
  protected ChatCompletionMessageParam createSystemMessage(String content) {
    return ChatCompletionMessageParam.ofSystem(
        ChatCompletionSystemMessageParam.builder()
            .content(ChatCompletionSystemMessageParam.Content.ofText(content))
            .build());
  }

  @Override
  protected ChatCompletionMessageParam createDeveloperMessage(String content) {
    return ChatCompletionMessageParam.ofDeveloper(
        ChatCompletionDeveloperMessageParam.builder()
            .content(ChatCompletionDeveloperMessageParam.Content.ofText(content))
            .build());
  }

  @Override
  protected ChatCompletionMessageParam createToolMessage(String response, String id) {
    return ChatCompletionMessageParam.ofTool(
        ChatCompletionToolMessageParam.builder()
            .toolCallId(id)
            .content(ChatCompletionToolMessageParam.Content.ofText(response))
            .build());
  }
}
