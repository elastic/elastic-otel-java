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

import static co.elastic.otel.openai.v1_1.wrappers.GenAiAttributes.GEN_AI_SYSTEM;

import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.context.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ChatCompletionEventsHelper {

  private static final java.util.logging.Logger LOG =
      java.util.logging.Logger.getLogger(ChatCompletionEventsHelper.class.getName());

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
      if (msg.isSystem()) {
        eventType = "gen_ai.system.message";
        if (settings.captureMessageContent) {
          putIfNotEmpty(bodyBuilder, "content", contentToString(msg.asSystem().content()));
        }
      } else if (msg.isDeveloper()) {
        eventType = "gen_ai.system.message";
        putIfNotEmpty(bodyBuilder, "role", "developer");
        if (settings.captureMessageContent) {
          putIfNotEmpty(bodyBuilder, "content", contentToString(msg.asDeveloper().content()));
        }
      } else if (msg.isUser()) {
        eventType = "gen_ai.user.message";
        if (settings.captureMessageContent) {
          putIfNotEmpty(bodyBuilder, "content", contentToString(msg.asUser().content()));
        }
      } else if (msg.isAssistant()) {
        ChatCompletionAssistantMessageParam assistantMsg = msg.asAssistant();
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
      } else if (msg.isTool()) {
        ChatCompletionToolMessageParam toolMsg = msg.asTool();
        eventType = "gen_ai.tool.message";
        if (settings.captureMessageContent) {
          putIfNotEmpty(bodyBuilder, "content", contentToString(toolMsg.content()));
          bodyBuilder.put("id", toolMsg.toolCallId());
        }
      } else {
        LOG.log(Level.WARNING, "Unhandled OpenAI message type will be dropped: {0}", msg);
        continue;
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
    if (content.isText()) {
      return content.asText();
    } else if (content.isArrayOfContentParts()) {
      return joinContentParts(content.asArrayOfContentParts());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  private static String contentToString(ChatCompletionAssistantMessageParam.Content content) {
    if (content.isText()) {
      return content.asText();
    } else if (content.isArrayOfContentParts()) {
      return content.asArrayOfContentParts().stream()
          .map(
              part -> {
                if (part.isText()) {
                  return part.asText().text();
                }
                if (part.isRefusal()) {
                  return part.asRefusal().refusal();
                }
                return null;
              })
          .filter(Objects::nonNull)
          .collect(Collectors.joining());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  private static String contentToString(ChatCompletionSystemMessageParam.Content content) {
    if (content.isText()) {
      return content.asText();
    } else if (content.isArrayOfContentParts()) {
      return joinContentParts(content.asArrayOfContentParts());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  private static String contentToString(ChatCompletionDeveloperMessageParam.Content content) {
    if (content.isText()) {
      return content.asText();
    } else if (content.isArrayOfContentParts()) {
      return joinContentParts(content.asArrayOfContentParts());
    } else {
      throw new IllegalStateException("Unhandled content type for " + content);
    }
  }

  private static String joinContentParts(List<ChatCompletionContentPartText> contentParts) {
    return contentParts.stream()
        .map(ChatCompletionContentPartText::text)
        .collect(Collectors.joining());
  }

  private static String contentToString(ChatCompletionUserMessageParam.Content content) {
    if (content.isText()) {
      return content.asText();
    } else if (content.isArrayOfContentParts()) {
      return content.asArrayOfContentParts().stream()
          .map(part -> part.isText() ? part.asText().text() : null)
          .filter(Objects::nonNull)
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
    result.put("type", Value.of("function")); // "function" is the only currently supported type
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
