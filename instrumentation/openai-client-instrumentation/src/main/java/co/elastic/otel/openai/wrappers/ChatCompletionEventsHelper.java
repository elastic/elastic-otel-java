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

import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_SYSTEM;

import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionContentPart;
import com.openai.models.ChatCompletionContentPartText;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessage;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionMessageToolCall;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.context.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatCompletionEventsHelper {

  private static final Logger EV_LOGGER =
      GlobalOpenTelemetry.get().getLogsBridge().get(Constants.INSTRUMENTATION_NAME);

  private static final AttributeKey<String> EVENT_NAME_KEY = AttributeKey.stringKey("event.name");

  public static void emitPromptLogEvents(
      ChatCompletionCreateParams request, InstrumentationSettings settings) {
    if (!settings.emitEvents) {
      return;
    }
    for (ChatCompletionMessageParam msg : request.messages()) {
      String eventType;
      MapValueBuilder bodyBuilder = new MapValueBuilder();
      if (msg.isChatCompletionSystemMessageParam()) {
        ChatCompletionSystemMessageParam sysMsg = msg.asChatCompletionSystemMessageParam();
        eventType = "gen_ai.system.message";
        if (settings.captureMessageContent) {
          putIfNotEmpty(bodyBuilder, "content", contentToString(sysMsg.content()));
        }
      } else if (msg.isChatCompletionUserMessageParam()) {
        ChatCompletionUserMessageParam userMsg = msg.asChatCompletionUserMessageParam();
        eventType = "gen_ai.user.message";
        if (settings.captureMessageContent) {
          putIfNotEmpty(bodyBuilder, "content", contentToString(userMsg.content()));
        }
      } else if (msg.isChatCompletionAssistantMessageParam()) {
        ChatCompletionAssistantMessageParam assistantMsg =
            msg.asChatCompletionAssistantMessageParam();
        eventType = "gen_ai.assistant.message";
        if (settings.captureMessageContent) {
          assistantMsg
              .content()
              .ifPresent(
                  content -> putIfNotEmpty(bodyBuilder, "content", contentToString(content)));
          assistantMsg
              .toolCalls()
              .ifPresent(
                  toolCalls -> {
                    List<Value<?>> toolCallsJson =
                        toolCalls.stream()
                            .map(ChatCompletionEventsHelper::buildToolCallEventObject)
                            .collect(Collectors.toList());
                    bodyBuilder.put("tool_calls", Value.of(toolCallsJson));
                  });
        }
      } else if (msg.isChatCompletionToolMessageParam()) {
        ChatCompletionToolMessageParam toolMsg = msg.asChatCompletionToolMessageParam();
        eventType = "gen_ai.tool.message";
        if (settings.captureMessageContent) {
          putIfNotEmpty(bodyBuilder, "content", contentToString(toolMsg.content()));
          bodyBuilder.put("id", toolMsg.toolCallId());
        }
      } else {
        throw new IllegalStateException("Unhandled type : " + msg.getClass().getName());
      }
      newEvent(eventType).setBody(bodyBuilder.build()).emit();
    }
  }

  private static void putIfNotEmpty(MapValueBuilder bodyBuilder, String key, String value) {
    if (value != null && !value.isEmpty()) {
      bodyBuilder.put(key, value);
    }
  }

  private static String contentToString(ChatCompletionToolMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    } else if (content.isArrayOfContentParts()) {
      return content.asArrayOfContentParts().stream()
          .map(ChatCompletionContentPartText::text)
          .collect(Collectors.joining());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  private static String contentToString(ChatCompletionAssistantMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    } else if (content.isArrayOfContentParts()) {
      return content.asArrayOfContentParts().stream()
          .map(
              cnt -> {
                if (cnt.isChatCompletionContentPartText()) {
                  return cnt.asChatCompletionContentPartText().text();
                } else if (cnt.isChatCompletionContentPartRefusal()) {
                  return cnt.asChatCompletionContentPartRefusal().refusal();
                }
                return "";
              })
          .collect(Collectors.joining());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  private static String contentToString(ChatCompletionSystemMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    } else if (content.isArrayOfContentParts()) {
      return content.asArrayOfContentParts().stream()
          .map(ChatCompletionContentPartText::text)
          .collect(Collectors.joining());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  private static String contentToString(ChatCompletionUserMessageParam.Content content) {
    if (content.isTextContent()) {
      return content.asTextContent();
    } else if (content.isArrayOfContentParts()) {
      return content.asArrayOfContentParts().stream()
          .filter(ChatCompletionContentPart::isChatCompletionContentPartText)
          .map(ChatCompletionContentPart::asChatCompletionContentPartText)
          .map(ChatCompletionContentPartText::text)
          .collect(Collectors.joining());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  public static void emitCompletionLogEvents(
      ChatCompletion completion, InstrumentationSettings settings) {
    if (!settings.emitEvents) {
      return;
    }
    for (ChatCompletion.Choice choice : completion.choices()) {
      ChatCompletionMessage choiceMsg = choice.message();
      Map<String, Value<?>> message = new HashMap<>();
      if (settings.captureMessageContent) {
        choiceMsg.content().ifPresent(content -> message.put("content", Value.of(content)));
        choiceMsg
            .toolCalls()
            .ifPresent(
                toolCalls -> {
                  message.put(
                      "tool_calls",
                      Value.of(
                          toolCalls.stream()
                              .map(ChatCompletionEventsHelper::buildToolCallEventObject)
                              .collect(Collectors.toList())));
                });
      }
      emitCompletionLogEvent(
          choice.index(), choice.finishReason().toString(), Value.of(message), settings, null);
    }
  }

  public static void emitCompletionLogEvent(
      long index,
      String finishReason,
      Value<?> eventMessageObject,
      InstrumentationSettings settings,
      Context contextOverride) {
    if (!settings.emitEvents) {
      return;
    }
    LogRecordBuilder builder =
        newEvent("gen_ai.choice")
            .setBody(
                new MapValueBuilder()
                    .put("finish_reason", finishReason)
                    .put("index", index)
                    .put("message", eventMessageObject)
                    .build());
    if (contextOverride != null) {
      builder.setContext(contextOverride);
    }
    builder.emit();
  }

  private static LogRecordBuilder newEvent(String name) {
    return EV_LOGGER
        .logRecordBuilder()
        .setAttribute(EVENT_NAME_KEY, name)
        .setAttribute(GEN_AI_SYSTEM, "openai");
  }

  private static Value<?> buildToolCallEventObject(ChatCompletionMessageToolCall call) {
    Map<String, Value<?>> result = new HashMap<>();
    result.put("id", Value.of(call.id()));
    result.put("type", Value.of(call.type().toString()));
    result.put("function", buildFunctionEventObject(call.function()));
    return Value.of(result);
  }

  private static Value<?> buildFunctionEventObject(
      ChatCompletionMessageToolCall.Function function) {
    Map<String, Value<?>> result = new HashMap<>();
    result.put("name", Value.of(function.name()));
    result.put("arguments", Value.of(function.arguments()));
    return Value.of(result);
  }
}
