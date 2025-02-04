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
package co.elastic.otel.openai;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPENAI_REQUEST_RESPONSE_FORMAT;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPENAI_REQUEST_SEED;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_FREQUENCY_PENALTY;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_PRESENCE_PENALTY;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_STOP_SEQUENCES;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_ID;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_SYSTEM;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.COMPLETION;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.INPUT;

import co.elastic.otel.openai.wrappers.Constants;
import co.elastic.otel.openai.wrappers.InstrumentationSettingsAccessor;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonObject;
import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.errors.NotFoundException;
import com.openai.errors.OpenAIIoException;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionMessageToolCall;
import com.openai.models.ChatCompletionStreamOptions;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionTool;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.ResponseFormatText;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ChatTestBase {
  private static final String TEST_CHAT_MODEL = "gpt-4o-mini";
  private static final String TEST_CHAT_RESPONSE_MODEL = "gpt-4o-mini-2024-07-18";
  private static final String TEST_CHAT_INPUT =
      "Answer in up to 3 words: Which ocean contains Bouvet Island?";

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension
  static final OpenAIRecordingExtension openai = new OpenAIRecordingExtension("ChatTest");

  @BeforeEach
  void setup() {
    // Default tests to capture content.
    // TODO: Change test default to false.
    InstrumentationSettingsAccessor.setCaptureMessageContent(openai.client, true);
    // Default tests to enable events.
    InstrumentationSettingsAccessor.setEmitEvents(openai.client, true);
  }

  static Consumer<HistogramPointData> assertThatDurationIsLessThan(long toNanos) {
    double nanosPerSecond = Duration.ofSeconds(1).toNanos();
    return point -> assertThat(point.getSum()).isStrictlyBetween(0.0, toNanos / nanosPerSecond);
  }

  private static void assertThatValueIsEmptyMap(Value<?> value) {
    assertThat(value.getValue()).isInstanceOfSatisfying(List.class, l -> assertThat(l).isEmpty());
  }

  private static void assertThatChoiceBodyHasNoContent(Value<?> body, String finishReason) {
    ValAssert.map().entry("index", 0).entry("finish_reason", finishReason).accept("", body);
    Value<?> message =
        ((List<KeyValue>) body.getValue())
            .stream().filter(kv -> kv.getKey().equals("message")).findFirst().get().getValue();
    assertThatValueIsEmptyMap(message);
  }

  @Test
  void chat() throws Exception {
    InstrumentationSettingsAccessor.setCaptureMessageContent(openai.client, false);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion chatCompletion = openai.client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;
    chatCompletion.validate();

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsExactly("stop");
                      })
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 22L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 3L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.user.message"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(
                      b -> {
                        assertThat(((List<KeyValue>) b.getValue())).isEmpty();
                      });
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.choice"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(b -> assertThatChoiceBodyHasNoContent(b, "stop"));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(22.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(3.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void allTheClientOptions() {
    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .frequencyPenalty(0.0)
            .maxTokens(100)
            .presencePenalty(0.0)
            .temperature(1.0)
            .topP(1.0)
            .stopOfStrings(Collections.singletonList("foo"))
            .seed(100L)
            .responseFormat(ResponseFormatText.builder().build())
            .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion result = openai.client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;
    result.validate();

    String content = "Southern Ocean.";
    assertThat(result.choices().get(0).message().content()).hasValue(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_OPENAI_REQUEST_SEED, 100L)
                  .hasAttribute(GEN_AI_OPENAI_REQUEST_RESPONSE_FORMAT, "text")
                  .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
                  .hasAttribute(GEN_AI_REQUEST_FREQUENCY_PENALTY, 0.0)
                  .hasAttribute(GEN_AI_REQUEST_MAX_TOKENS, 100L)
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_REQUEST_PRESENCE_PENALTY, 0.0)
                  .hasAttribute(GEN_AI_REQUEST_STOP_SEQUENCES, Collections.singletonList("foo"))
                  .hasAttribute(GEN_AI_REQUEST_TEMPERATURE, 1.0)
                  .hasAttribute(GEN_AI_REQUEST_TOP_P, 1.0)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsExactly("stop");
                      })
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 22L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 3L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.user.message"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", TEST_CHAT_INPUT));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.choice"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("index", 0)
                          .entry("finish_reason", "stop")
                          .entry("message", ValAssert.map().entry("content", content)));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(22.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(3.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void multipleChoicesWithCaptureContent() {
    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .n(2)
            .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion result = openai.client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;
    result.validate();

    String content = "Atlantic Ocean.";
    assertThat(result.choices().get(0).message().content()).hasValue(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsExactly("stop", "stop");
                      })
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 22L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 9L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(3)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.user.message"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", TEST_CHAT_INPUT));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.choice"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("index", 0)
                          .entry("finish_reason", "stop")
                          .entry("message", ValAssert.map().entry("content", content)));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.choice"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("index", 1)
                          .entry("finish_reason", "stop")
                          .entry(
                              "message",
                              ValAssert.map().entry("content", "South Atlantic Ocean.")));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(22.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(9.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  // TODO: JSON strings are currently asserted by value, so different spacing from different
  // providers
  // can cause errors. They need to be updated to JSON assertions.
  @Test
  void toolCalls() {
    InstrumentationSettingsAccessor.setCaptureMessageContent(openai.client, false);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(
                Arrays.asList(
                    createSystemMessage(
                        "You are a helpful customer support assistant. Use the supplied tools to assist the user."),
                    createUserMessage("Hi, can you tell me the delivery date for my order?"),
                    createAssistantMessage(
                        "Hi there! I can help with that. Can you please provide your order ID?"),
                    createUserMessage("i think it is order_12345")))
            .model(TEST_CHAT_MODEL)
            .addTool(buildGetDeliveryDateToolDefinition())
            .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion response = openai.client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;
    response.validate();

    List<ChatCompletionMessageToolCall> toolCalls =
        response.choices().get(0).message().toolCalls().get();

    assertThat(toolCalls).hasSize(1);
    assertThat(toolCalls.get(0))
        .satisfies(
            call -> {
              assertThat(call.function().name()).isEqualTo("get_delivery_date");
              assertThat(call.function().arguments()).isEqualTo("{\"order_id\":\"order_12345\"}");
            });

    assertThat(testing.spans())
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(
                      GEN_AI_RESPONSE_FINISH_REASONS, Collections.singletonList("tool_calls"))
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 140L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 20L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    assertThat(testing.logRecords())
        .hasSize(5)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.system.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue()).satisfies(ChatTestBase::assertThatValueIsEmptyMap);
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue()).satisfies(ChatTestBase::assertThatValueIsEmptyMap);
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.assistant.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue()).satisfies(ChatTestBase::assertThatValueIsEmptyMap);
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue()).satisfies(ChatTestBase::assertThatValueIsEmptyMap);
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(b -> assertThatChoiceBodyHasNoContent(b, "tool_calls"));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(140.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(20.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void toolCallsWithCaptureMessageContent() {
    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(
                Arrays.asList(
                    createSystemMessage(
                        "You are a helpful customer support assistant. Use the supplied tools to assist the user."),
                    createUserMessage("Hi, can you tell me the delivery date for my order?"),
                    createAssistantMessage(
                        "Hi there! I can help with that. Can you please provide your order ID?"),
                    createUserMessage("i think it is order_12345")))
            .model(TEST_CHAT_MODEL)
            .addTool(buildGetDeliveryDateToolDefinition())
            .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion response = openai.client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;
    response.validate();

    List<ChatCompletionMessageToolCall> toolCalls =
        response.choices().get(0).message().toolCalls().get();

    assertThat(toolCalls).hasSize(1);
    assertThat(toolCalls.get(0))
        .satisfies(
            call -> {
              assertThat(call.function().name()).isEqualTo("get_delivery_date");
              assertThat(call.function().arguments()).isEqualTo("{\"order_id\":\"order_12345\"}");
              assertThat(call.id()).startsWith("call_");
            });

    String toolCallId = toolCalls.get(0).id();

    assertThat(testing.spans())
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(
                      GEN_AI_RESPONSE_FINISH_REASONS, Collections.singletonList("tool_calls"))
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 140L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 20L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    assertThat(testing.logRecords())
        .hasSize(5)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.system.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry(
                              "content",
                              "You are a helpful customer support assistant. Use the supplied tools to assist the user."));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("content", "Hi, can you tell me the delivery date for my order?"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.assistant.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry(
                              "content",
                              "Hi there! I can help with that. Can you please provide your order ID?"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", "i think it is order_12345"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("finish_reason", "tool_calls")
                          .entry("index", 0)
                          .entry(
                              "message",
                              ValAssert.map()
                                  .entry(
                                      "tool_calls",
                                      ValAssert.array()
                                          .ignoreOrder()
                                          .entry(
                                              ValAssert.map()
                                                  .entry("id", toolCallId)
                                                  .entry("type", "function")
                                                  .entry(
                                                      "function",
                                                      ValAssert.map()
                                                          .entry("name", "get_delivery_date")
                                                          .entry(
                                                              "arguments",
                                                              "{\"order_id\":\"order_12345\"}"))))));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(140.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(20.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void connectionError() {
    OpenAIClient client =
        OpenAIOkHttpClient.builder().baseUrl("http://localhost:9999/v5").apiKey("testing").build();
    InstrumentationSettingsAccessor.setEmitEvents(client, true);
    InstrumentationSettingsAccessor.setCaptureMessageContent(client, true);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .build();

    Exception exception = null;
    long startTimeNanos = System.nanoTime();
    try {
      client.chat().completions().create(params);
    } catch (Exception e) {
      exception = e;
    }
    long durationNanos = System.nanoTime() - startTimeNanos;

    Exception finalException = exception;
    assertThat(finalException).isNotNull().isInstanceOf(OpenAIIoException.class);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasException(finalException)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, 9999L);
            });

    assertThat(testing.logRecords())
        .hasSize(1)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", TEST_CHAT_INPUT));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(ERROR_TYPE, finalException.getClass().getName()),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, 9999L)))));
  }

  @Test
  void connectionErrorStream() {
    OpenAIClient client =
        OpenAIOkHttpClient.builder()
            .baseUrl("http://localhost:" + openai.getPort() + "/bad-url")
            .apiKey("testing")
            .build();
    InstrumentationSettingsAccessor.setEmitEvents(client, true);
    InstrumentationSettingsAccessor.setCaptureMessageContent(client, true);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .build();

    Exception exception = null;
    long startTimeNanos = System.nanoTime();
    try {
      client.chat().completions().createStreaming(params);
    } catch (Exception e) {
      exception = e;
    }
    long durationNanos = System.nanoTime() - startTimeNanos;
    Exception finalException = exception;
    assertThat(finalException).isNotNull().isInstanceOf(NotFoundException.class);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasException(finalException)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    assertThat(testing.logRecords())
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", TEST_CHAT_INPUT));
            })
        .hasSize(1);

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(ERROR_TYPE, finalException.getClass().getName()),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void captureMessageContent() {

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion result = openai.client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;
    result.validate();

    String content = "Southern Ocean";
    assertThat(result.choices().get(0).message().content()).hasValue(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsExactly("stop");
                      })
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 22L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 3L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.user.message"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", TEST_CHAT_INPUT));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry(GEN_AI_SYSTEM, "openai")
                              .containsEntry("event.name", "gen_ai.choice"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("index", 0)
                          .entry("finish_reason", "stop")
                          .entry("message", ValAssert.map().entry("content", content)));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(22.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(3.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void stream() throws Exception {
    InstrumentationSettingsAccessor.setCaptureMessageContent(openai.client, false);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .build();

    long startTimeNanos = System.nanoTime();
    List<ChatCompletionChunk> chunks;
    try (StreamResponse<ChatCompletionChunk> result =
        openai.client.chat().completions().createStreaming(params)) {
      chunks = result.stream().collect(Collectors.toList());
    }
    long durationNanos = System.nanoTime() - startTimeNanos;

    String fullMessage =
        chunks.stream()
            .map(cc -> cc.choices().get(0).delta().content())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());

    String content = "South Atlantic Ocean.";
    assertThat(fullMessage).isEqualTo(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort())
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsAnyOf("stop");
                      });
            });
    SpanContext spanCtx = spans.get(0).getSpanContext();

    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue()).satisfies(ChatTestBase::assertThatValueIsEmptyMap);
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(b -> assertThatChoiceBodyHasNoContent(b, "stop"));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void streamWithIncludeUsage() throws Exception {
    InstrumentationSettingsAccessor.setCaptureMessageContent(openai.client, false);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .streamOptions(ChatCompletionStreamOptions.builder().includeUsage(true).build())
            .build();

    long startTimeNanos = System.nanoTime();
    List<ChatCompletionChunk> chunks;
    try (StreamResponse<ChatCompletionChunk> result =
        openai.client.chat().completions().createStreaming(params)) {
      chunks = result.stream().collect(Collectors.toList());
    }
    long durationNanos = System.nanoTime() - startTimeNanos;

    String fullMessage =
        chunks.stream()
            .filter(cc -> !cc.choices().isEmpty())
            .map(cc -> cc.choices().get(0).delta().content())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());

    String content = "South Atlantic Ocean.";
    assertThat(fullMessage).isEqualTo(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsExactly("stop");
                      })
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 22L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 4L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });
    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue()).satisfies(ChatTestBase::assertThatValueIsEmptyMap);
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(b -> assertThatChoiceBodyHasNoContent(b, "stop"));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(22.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(4.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void streamAllTheClientOptions() throws Exception {
    InstrumentationSettingsAccessor.setCaptureMessageContent(openai.client, false);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .frequencyPenalty(0.0)
            .maxTokens(100)
            .presencePenalty(0.0)
            .temperature(1.0)
            .topP(1.0)
            .stopOfStrings(Collections.singletonList("foo"))
            .seed(100L)
            .responseFormat(ResponseFormatText.builder().build())
            .build();

    long startTimeNanos = System.nanoTime();
    List<ChatCompletionChunk> chunks;
    try (StreamResponse<ChatCompletionChunk> result =
        openai.client.chat().completions().createStreaming(params)) {
      chunks = result.stream().collect(Collectors.toList());
    }
    long durationNanos = System.nanoTime() - startTimeNanos;

    String fullMessage =
        chunks.stream()
            .map(cc -> cc.choices().get(0).delta().content())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());

    String content = "Southern Ocean.";
    assertThat(fullMessage).isEqualTo(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_OPENAI_REQUEST_SEED, 100L)
                  .hasAttribute(GEN_AI_OPENAI_REQUEST_RESPONSE_FORMAT, "text")
                  .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
                  .hasAttribute(GEN_AI_REQUEST_FREQUENCY_PENALTY, 0.0)
                  .hasAttribute(GEN_AI_REQUEST_MAX_TOKENS, 100L)
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_REQUEST_PRESENCE_PENALTY, 0.0)
                  .hasAttribute(GEN_AI_REQUEST_STOP_SEQUENCES, Collections.singletonList("foo"))
                  .hasAttribute(GEN_AI_REQUEST_TEMPERATURE, 1.0)
                  .hasAttribute(GEN_AI_REQUEST_TOP_P, 1.0)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsExactly("stop");
                      })
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });
    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue()).satisfies(ChatTestBase::assertThatValueIsEmptyMap);
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(b -> assertThatChoiceBodyHasNoContent(b, "stop"));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void streamWithCaptureMessageContent() throws Exception {
    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .build();

    long startTimeNanos = System.nanoTime();
    List<ChatCompletionChunk> chunks;
    try (StreamResponse<ChatCompletionChunk> result =
        openai.client.chat().completions().createStreaming(params)) {
      chunks = result.stream().collect(Collectors.toList());
    }
    long durationNanos = System.nanoTime() - startTimeNanos;

    String fullMessage =
        chunks.stream()
            .map(cc -> cc.choices().get(0).delta().content())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());

    String content = "South Atlantic Ocean.";
    assertThat(fullMessage).isEqualTo(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort())
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsAnyOf("stop");
                      });
            });
    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", TEST_CHAT_INPUT));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"))
                  .hasSpanContext(spanCtx);
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("finish_reason", "stop")
                          .entry("index", 0)
                          .entry("message", ValAssert.map().entry("content", content)));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void streamToolsAndCaptureMessageContent() throws Exception {
    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(
                Arrays.asList(
                    createSystemMessage(
                        "You are a helpful customer support assistant. Use the supplied tools to assist the user."),
                    createUserMessage("Hi, can you tell me the delivery date for my order?"),
                    createAssistantMessage(
                        "Hi there! I can help with that. Can you please provide your order ID?"),
                    createUserMessage("i think it is order_12345")))
            .model(TEST_CHAT_MODEL)
            .addTool(buildGetDeliveryDateToolDefinition())
            .build();

    long startTimeNanos = System.nanoTime();
    List<ChatCompletionChunk> chunks;
    try (StreamResponse<ChatCompletionChunk> result =
        openai.client.chat().completions().createStreaming(params)) {
      chunks = result.stream().collect(Collectors.toList());
    }
    long durationNanos = System.nanoTime() - startTimeNanos;

    String fullMessage =
        chunks.stream()
            .map(cc -> cc.choices().get(0).delta().content())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining());
    assertThat(fullMessage).isEmpty();

    String toolCallId =
        chunks.stream()
            .map(chunk -> chunk.choices().get(0).delta().toolCalls())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(tool -> tool.get(0).id())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .get();

    assertThat(testing.spans())
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(
                      GEN_AI_RESPONSE_FINISH_REASONS, Collections.singletonList("tool_calls"))
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    assertThat(testing.logRecords())
        .hasSize(5)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.system.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry(
                              "content",
                              "You are a helpful customer support assistant. Use the supplied tools to assist the user."));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("content", "Hi, can you tell me the delivery date for my order?"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.assistant.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry(
                              "content",
                              "Hi there! I can help with that. Can you please provide your order ID?"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(ValAssert.map().entry("content", "i think it is order_12345"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("finish_reason", "tool_calls")
                          .entry("index", 0)
                          .entry(
                              "message",
                              ValAssert.map()
                                  .entry(
                                      "tool_calls",
                                      ValAssert.array()
                                          .ignoreOrder()
                                          .entry(
                                              ValAssert.map()
                                                  .entry("id", toolCallId)
                                                  .entry("type", "function")
                                                  .entry(
                                                      "function",
                                                      ValAssert.map()
                                                          .entry("name", "get_delivery_date")
                                                          .entry(
                                                              "arguments",
                                                              "{\"order_id\":\"order_12345\"}"))))));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanos))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  // TODO: recheck for OpenAi client if this test is now possible
  /*

  @ParameterizedTest
  @ValueSource(booleans = { true, false})
  @Disabled("Azure SDK does not allow implementing correctly - https://github.com/Azure/azure-sdk-for-java/issues/43611")
  void streamParallelToolsAndCaptureMessageContent(boolean useResponseApi) {
      ChatCompletionsFunctionToolDefinitionFunction functionDefinition = new ChatCompletionsFunctionToolDefinitionFunction("get_weather");
      GetWeatherFunctionParameterDefinition
              parameters = new GetWeatherFunctionParameterDefinition();
      functionDefinition.setParameters(BinaryData.fromObject(parameters));

      List<ChatRequestMessage> chatMessages = new ArrayList<>();
      chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant providing weather updates."));
      chatMessages.add(new ChatRequestUserMessage("What is the weather in New York City and London?"));

      ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages)
              .setTools(List.of(new ChatCompletionsFunctionToolDefinition(functionDefinition)));


      long startTimeNanos = System.nanoTime();
      IterableStream<ChatCompletions> chatCompletionsStream;
      if (useResponseApi) {
          chatCompletionsStream = openai.client.getChatCompletionsStreamWithResponse(TEST_CHAT_MODEL, options, new RequestOptions())
                                               .getValue();
      } else {
          chatCompletionsStream = openai.client.getChatCompletionsStream(TEST_CHAT_MODEL, options);
      }
      List<ChatCompletions> completions = chatCompletionsStream.stream().toList();
      long durationNanos = System.nanoTime() - startTimeNanos;

      String fullMessage = completions.stream()
                                      .map(cc -> cc.getChoices().get(0).getDelta())
                                      .filter(Objects::nonNull)
                                      .map(ChatResponseMessage::getContent)
                                      .filter(Objects::nonNull)
                                      .collect(Collectors.joining());
      assertThat(fullMessage).isEmpty();


      assertThat(testing.spans())
              .hasSize(1)
              .first()
              .satisfies(span -> {
                  assertThat(span)
                          .hasAttribute(GEN_AI_SYSTEM, "openai")
                          .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                          .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                          .hasAttribute(GEN_AI_RESPONSE_ID, "chatcmpl-AiWm43aBO7oeXWZ3bOGNh48zGSfDN")
                          .hasAttribute(GEN_AI_RESPONSE_FINISH_REASONS, List.of("tool_calls"))
                          .hasAttribute(SERVER_ADDRESS, "localhost")
                          .hasAttribute(SERVER_PORT, (long) openai.getPort());
              });

      assertThat(testing.logRecords())
              .hasSize(3)
              .anySatisfy(log -> {
                  assertThat(log)
                          .hasAttributesSatisfying(attr -> assertThat(attr)
                                  .containsEntry("event.name", "gen_ai.system.message")
                                  .containsEntry(GEN_AI_SYSTEM, "openai")
                          );
                  assertThat(log.getBodyValue())
                          .satisfies(ValAssert.map()
                                              .entry("content", "You are a helpful assistant providing weather updates.")
                          );
              })
              .anySatisfy(log -> {
                  assertThat(log)
                          .hasAttributesSatisfying(attr -> assertThat(attr)
                                  .containsEntry("event.name", "gen_ai.user.message")
                                  .containsEntry(GEN_AI_SYSTEM, "openai")
                          );
                  assertThat(log.getBodyValue())
                          .satisfies(ValAssert.map()
                                              .entry("content", "What is the weather in New York City and London?")
                          );
              })
              .anySatisfy(log -> {
                  assertThat(log)
                          .hasAttributesSatisfying(attr -> assertThat(attr)
                                  .containsEntry("event.name", "gen_ai.choice")
                                  .containsEntry(GEN_AI_SYSTEM, "openai")
                          );
                  assertThat(log.getBodyValue())
                          .satisfies(ValAssert.map()
                                              .entry("finish_reason", "tool_calls")
                                              .entry("index", 0)
                                              .entry("message", ValAssert.map()
                                                                         .entry("tool_calls", ValAssert.array().ignoreOrder()
                                                                                                       .entry(ValAssert.map()
                                                                                                                       .entry("id", "call_HtKN6juSPlIW7Jn6wGu9Fl98")
                                                                                                                       .entry("type", "function")
                                                                                                                       .entry("function", ValAssert.map()
                                                                                                                                                   .entry("name", "get_weather")
                                                                                                                                                   .entry("arguments", "{\"location\": \"New York City\"}")
                                                                                                                       ))
                                                                                                       .entry(ValAssert.map()
                                                                                                                       .entry("id", "call_YJvXI6DrRWpyOXEGQmX8sX30")
                                                                                                                       .entry("type", "function")
                                                                                                                       .entry("function", ValAssert.map()
                                                                                                                                                   .entry("name", "get_weather")
                                                                                                                                                   .entry("arguments", "{\"location\": \"London\"}")
                                                                                                                       ))
                                                                         )
                                              )
                          );
              });

      testing.waitAndAssertMetrics(
              Constants.INSTRUMENTATION_NAME,
              metric -> metric.hasName("gen_ai.client.operation.duration")
                              .hasHistogramSatisfying(
                                      histogram -> histogram.hasPointsSatisfying(
                                              point -> point
                                                      .satisfies(assertThatDurationIsLessThan(durationNanos))
                                                      .hasAttributesSatisfyingExactly(
                                                              equalTo(GEN_AI_SYSTEM, "openai"),
                                                              equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                                              equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                                              equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                                              equalTo(SERVER_ADDRESS, "localhost"),
                                                              equalTo(SERVER_PORT, (long) openai.getPort())
                                                      )
                                      )
                              ));
  }
  */

  @Test
  void toolsWithFollowupAndCaptureContent() {

    List<ChatCompletionMessageParam> chatMessages = new ArrayList<>();
    chatMessages.add(createSystemMessage("You are a helpful assistant providing weather updates."));
    chatMessages.add(createUserMessage("What is the weather in New York City and London?"));

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(chatMessages)
            .model(TEST_CHAT_MODEL)
            .addTool(buildGetWeatherToolDefinition())
            .build();

    long startTimeNanosFirst = System.nanoTime();
    ChatCompletion response = openai.client.chat().completions().create(params);
    long durationNanosFirst = System.nanoTime() - startTimeNanosFirst;
    response.validate();

    assertThat(response.choices().get(0).message().content()).isEmpty();

    List<ChatCompletionMessageToolCall> toolCalls =
        response.choices().get(0).message().toolCalls().get();
    assertThat(toolCalls).hasSize(2);
    String newYorkCallId =
        toolCalls.stream()
            .filter(call -> call.function().arguments().contains("New York"))
            .map(ChatCompletionMessageToolCall::id)
            .findFirst()
            .get();
    String londonCallId =
        toolCalls.stream()
            .filter(call -> call.function().arguments().contains("London"))
            .map(ChatCompletionMessageToolCall::id)
            .findFirst()
            .get();

    assertThat(newYorkCallId).startsWith("call_");
    assertThat(londonCallId).startsWith("call_");

    assertThat(testing.spans())
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(
                      GEN_AI_RESPONSE_FINISH_REASONS, Collections.singletonList("tool_calls"))
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    assertThat(testing.logRecords())
        .hasSize(3)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.system.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry(
                              "content", "You are a helpful assistant providing weather updates."));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("content", "What is the weather in New York City and London?"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("finish_reason", "tool_calls")
                          .entry("index", 0)
                          .entry(
                              "message",
                              ValAssert.map()
                                  .entry(
                                      "tool_calls",
                                      ValAssert.array()
                                          .ignoreOrder()
                                          .entry(
                                              ValAssert.map()
                                                  .entry("id", newYorkCallId)
                                                  .entry("type", "function")
                                                  .entry(
                                                      "function",
                                                      ValAssert.map()
                                                          .entry("name", "get_weather")
                                                          .entry(
                                                              "arguments",
                                                              "{\"location\": \"New York City\"}")))
                                          .entry(
                                              ValAssert.map()
                                                  .entry("id", londonCallId)
                                                  .entry("type", "function")
                                                  .entry(
                                                      "function",
                                                      ValAssert.map()
                                                          .entry("name", "get_weather")
                                                          .entry(
                                                              "arguments",
                                                              "{\"location\": \"London\"}"))))));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanosFirst))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(67.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(47.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
    testing.clearData();

    ChatCompletionMessageParam assistantMessage =
        ChatCompletionMessageParam.ofChatCompletionAssistantMessageParam(
            ChatCompletionAssistantMessageParam.builder()
                .content(ChatCompletionAssistantMessageParam.Content.ofTextContent(""))
                .toolCalls(toolCalls)
                .build());

    chatMessages.add(assistantMessage);
    chatMessages.add(createToolMessage("25 degrees and sunny", newYorkCallId));
    chatMessages.add(createToolMessage("15 degrees and raining", londonCallId));

    long startTimeNanosSecond = System.nanoTime();
    ChatCompletion fullCompletion =
        openai
            .client
            .chat()
            .completions()
            .create(
                ChatCompletionCreateParams.builder()
                    .messages(chatMessages)
                    .model(TEST_CHAT_MODEL)
                    .build());
    long durationNanosSecond = System.nanoTime() - startTimeNanosSecond;
    fullCompletion.validate();

    ChatCompletion.Choice finalChoice = fullCompletion.choices().get(0);
    String finalAnswer = finalChoice.message().content().get();

    assertThat(testing.spans())
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_FINISH_REASONS, Collections.singletonList("stop"))
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    assertThat(testing.logRecords())
        .hasSize(6)
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.system.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry(
                              "content", "You are a helpful assistant providing weather updates."));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.user.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("content", "What is the weather in New York City and London?"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.assistant.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry(
                              "tool_calls",
                              ValAssert.array()
                                  .entry(
                                      ValAssert.map()
                                          .entry("id", newYorkCallId)
                                          .entry("type", "function")
                                          .entry(
                                              "function",
                                              ValAssert.map()
                                                  .entry("name", "get_weather")
                                                  .entry(
                                                      "arguments",
                                                      "{\"location\": \"New York City\"}")))
                                  .entry(
                                      ValAssert.map()
                                          .entry("id", londonCallId)
                                          .entry("type", "function")
                                          .entry(
                                              "function",
                                              ValAssert.map()
                                                  .entry("name", "get_weather")
                                                  .entry(
                                                      "arguments",
                                                      "{\"location\": \"London\"}")))));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.tool.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("id", newYorkCallId)
                          .entry("content", "25 degrees and sunny"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.tool.message")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("id", londonCallId)
                          .entry("content", "15 degrees and raining"));
            })
        .anySatisfy(
            log -> {
              assertThat(log)
                  .hasAttributesSatisfying(
                      attr ->
                          assertThat(attr)
                              .containsEntry("event.name", "gen_ai.choice")
                              .containsEntry(GEN_AI_SYSTEM, "openai"));
              assertThat(log.getBodyValue())
                  .satisfies(
                      ValAssert.map()
                          .entry("finish_reason", finalChoice.finishReason().toString())
                          .entry("index", 0)
                          .entry("message", ValAssert.map().entry("content", finalAnswer)));
            });

    testing.waitAndAssertMetrics(
        Constants.INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("gen_ai.client.operation.duration")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .satisfies(assertThatDurationIsLessThan(durationNanosSecond))
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))),
        metric ->
            metric
                .hasName("gen_ai.client.token.usage")
                .hasHistogramSatisfying(
                    histogram ->
                        histogram.hasPointsSatisfying(
                            point ->
                                point
                                    .hasSum(99.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, INPUT),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())),
                            point ->
                                point
                                    .hasSum(27.0)
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(GEN_AI_SYSTEM, "openai"),
                                        equalTo(GEN_AI_OPERATION_NAME, "chat"),
                                        equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                                        equalTo(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL),
                                        equalTo(GEN_AI_TOKEN_TYPE, COMPLETION),
                                        equalTo(SERVER_ADDRESS, "localhost"),
                                        equalTo(SERVER_PORT, (long) openai.getPort())))));
  }

  @Test
  void disableEvents() {
    // Override the enablement from setup()
    InstrumentationSettingsAccessor.setEmitEvents(openai.client, false);

    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .messages(Collections.singletonList(createUserMessage(TEST_CHAT_INPUT)))
            .model(TEST_CHAT_MODEL)
            .build();

    ChatCompletion result = openai.client.chat().completions().create(params);
    result.validate();

    String content = "Southern Ocean";
    assertThat(result.choices().get(0).message().content()).hasValue(content);

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(
            span -> {
              assertThat(span)
                  .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
                  .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
                  .hasAttribute(GEN_AI_SYSTEM, "openai")
                  .hasAttributesSatisfying(
                      att -> assertThat(att.get(GEN_AI_RESPONSE_ID)).startsWith("chatcmpl-"))
                  .hasAttribute(GEN_AI_RESPONSE_MODEL, TEST_CHAT_RESPONSE_MODEL)
                  .hasAttributesSatisfying(
                      attribs -> {
                        assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                            .containsExactly("stop");
                      })
                  .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, 22L)
                  .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, 3L)
                  .hasAttribute(SERVER_ADDRESS, "localhost")
                  .hasAttribute(SERVER_PORT, (long) openai.getPort());
            });

    assertThat(testing.logRecords()).isEmpty();
  }

  private static ChatCompletionTool buildGetWeatherToolDefinition() {
    Map<String, JsonValue> location = new HashMap<>();
    location.put("type", JsonValue.from("string"));
    location.put("description", JsonValue.from("The location to get the current temperature for"));

    Map<String, JsonValue> properties = new HashMap<>();
    properties.put("location", JsonObject.of(location));

    return ChatCompletionTool.builder()
        .function(
            FunctionDefinition.builder()
                .name("get_weather")
                .parameters(
                    FunctionParameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                            "required", JsonValue.from(Collections.singletonList("location")))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .putAdditionalProperty("properties", JsonObject.of(properties))
                        .build())
                .build())
        .build();
  }

  static ChatCompletionTool buildGetDeliveryDateToolDefinition() {
    Map<String, JsonValue> orderId = new HashMap<>();
    orderId.put("type", JsonValue.from("string"));
    orderId.put("description", JsonValue.from("The customer's order ID."));

    Map<String, JsonValue> properties = new HashMap<>();
    properties.put("order_id", JsonObject.of(orderId));

    return ChatCompletionTool.builder()
        .function(
            FunctionDefinition.builder()
                .name("get_delivery_date")
                .description(
                    "Get the delivery date for a customer's order. "
                        + "Call this whenever you need to know the delivery date, for "
                        + "example when a customer asks 'Where is my package'")
                .parameters(
                    FunctionParameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                            "required", JsonValue.from(Collections.singletonList("order_id")))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .putAdditionalProperty("properties", JsonObject.of(properties))
                        .build())
                .build())
        .build();
  }

  private static ChatCompletionMessageParam createAssistantMessage(String content) {
    return ChatCompletionMessageParam.ofChatCompletionAssistantMessageParam(
        ChatCompletionAssistantMessageParam.builder()
            .content(ChatCompletionAssistantMessageParam.Content.ofTextContent(content))
            .build());
  }

  private static ChatCompletionMessageParam createUserMessage(String content) {
    return ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
        ChatCompletionUserMessageParam.builder()
            .content(ChatCompletionUserMessageParam.Content.ofTextContent(content))
            .build());
  }

  private static ChatCompletionMessageParam createSystemMessage(String content) {
    return ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
        ChatCompletionSystemMessageParam.builder()
            .content(ChatCompletionSystemMessageParam.Content.ofTextContent(content))
            .build());
  }

  private static ChatCompletionMessageParam createToolMessage(String response, String id) {
    return ChatCompletionMessageParam.ofChatCompletionToolMessageParam(
        ChatCompletionToolMessageParam.builder()
            .toolCallId(id)
            .content(ChatCompletionToolMessageParam.Content.ofTextContent(response))
            .build());
  }
}
