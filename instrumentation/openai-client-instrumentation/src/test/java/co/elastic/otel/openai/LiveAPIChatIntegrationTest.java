package co.elastic.otel.openai;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_ID;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_SYSTEM;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS;

import co.elastic.otel.openai.wrappers.InstrumentationSettingsAccessor;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionAssistantMessageParam;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionMessageToolCall;
import com.openai.models.ChatCompletionStreamOptions;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionToolMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import com.openai.models.CompletionUsage;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * This test runs against the real OpenAI-API. It therefore requires an API-key to be setup.
 * This test is currently not executed on CI.
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class LiveAPIChatIntegrationTest {
  private static final String TEST_CHAT_MODEL = System.getenv()
      .getOrDefault("TEST_CHAT_MODEL", "gpt-4o-mini");
  private static final String TEST_CHAT_INPUT = "Answer in up to 3 words: Which ocean contains Bouvet Island?";

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  private OpenAIClient client;
  private String clientHost;
  private long clientPort;

  @BeforeEach
  void setup() throws Exception {

    OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
        .apiKey(System.getenv().get("OPENAI_API_KEY"));
    String baseUrl = System.getenv().get("OPENAI_BASE_URL");
    if (baseUrl != null) {
      URL url = new URL(baseUrl);
      clientHost = url.getHost();
      clientPort = url.getPort();
      if (clientPort == -1) {
        clientPort = url.getDefaultPort();
      }
      builder.baseUrl(baseUrl);
    } else {
      clientHost = "api.openai.com";
      clientPort = 443;
    }
    client = builder.build();

    InstrumentationSettingsAccessor.setCaptureMessageContent(client, true);
    InstrumentationSettingsAccessor.setEmitEvents(client, true);
  }

  @Test
  void toolCallsWithCaptureMessageContent() {

    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .messages(Arrays.asList(
            createSystemMessage(
                "You are a helpful customer support assistant. Use the supplied tools to assist the user."),
            createUserMessage("Hi, can you tell me the delivery date for my order?"),
            createAssistantMessage(
                "Hi there! I can help with that. Can you please provide your order ID?"),
            createUserMessage("i think it is order_12345")
        ))
        .model(TEST_CHAT_MODEL)
        .addTool(ChatTest.buildGetDeliveryDateToolDefinition())
        .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion response = client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;

    List<ChatCompletionMessageToolCall> toolCalls = response.choices().get(0).message().toolCalls()
        .get();

    assertThat(toolCalls).hasSize(1);
    assertThat(toolCalls.get(0))
        .satisfies(call -> {
          assertThat(call.function().name()).isEqualTo("get_delivery_date");
          assertThat(call.function().arguments()).isEqualTo("{\"order_id\":\"order_12345\"}");
        });

    assertThat(testing.spans())
        .hasSize(1)
        .first()
        .satisfies(span -> {
          assertThat(span)
              .hasAttribute(GEN_AI_SYSTEM, "openai")
              .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
              .hasAttribute(GEN_AI_RESPONSE_MODEL, response.model())
              .hasAttribute(GEN_AI_RESPONSE_ID, response.id())
              .hasAttribute(GEN_AI_RESPONSE_FINISH_REASONS, Collections.singletonList("tool_calls"))
              .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, response.usage().get().promptTokens())
              .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, response.usage().get().completionTokens())
              .hasAttribute(SERVER_ADDRESS, clientHost)
              .hasAttribute(SERVER_PORT, clientPort);
        });

    assertThat(testing.logRecords())
        .hasSize(5)
        .anySatisfy(log -> {
          assertThat(log)
              .hasAttributesSatisfying(attr -> assertThat(attr)
                  .containsEntry("event.name", "gen_ai.system.message")
                  .containsEntry(GEN_AI_SYSTEM, "openai")
              );
          assertThat(log.getBodyValue())
              .satisfies(ValAssert.map()
                  .entry("content",
                      "You are a helpful customer support assistant. Use the supplied tools to assist the user.")
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
                  .entry("content",
                      "Hi, can you tell me the delivery date for my order?")
              );
        })
        .anySatisfy(log -> {
          assertThat(log)
              .hasAttributesSatisfying(attr -> assertThat(attr)
                  .containsEntry("event.name", "gen_ai.assistant.message")
                  .containsEntry(GEN_AI_SYSTEM, "openai")
              );
          assertThat(log.getBodyValue())
              .satisfies(ValAssert.map()
                  .entry("content",
                      "Hi there! I can help with that. Can you please provide your order ID?")
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
                  .entry("content", "i think it is order_12345")
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
                      .entry("tool_calls",
                          ValAssert.array()
                              .ignoreOrder()
                              .entry(ValAssert.map()
                                  .entry("id",
                                      toolCalls.get(0).id())
                                  .entry("type",
                                      "function")
                                  .entry("function",
                                      ValAssert.map()
                                          .entry("name",
                                              "get_delivery_date")
                                          .entry("arguments",
                                              "{\"order_id\":\"order_12345\"}")
                                  ))
                      )
                  )
              );
        });

    testing.waitAndAssertMetrics(
        "openai-client",
        metric -> metric.hasName("gen_ai.client.operation.duration")
            .hasHistogramSatisfying(
                histogram -> histogram.hasPointsSatisfying(
                    point -> point
                        .satisfies(assertThatDurationIsLessThan(durationNanos))
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL,
                                response.model()),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        )
                )
            ),
        metric -> metric.hasName("gen_ai.client.token.usage")
            .hasHistogramSatisfying(
                histogram -> histogram.hasPointsSatisfying(
                    point -> point
                        .hasSum(response.usage().get().promptTokens())
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL,
                                response.model()),
                            equalTo(GEN_AI_TOKEN_TYPE,
                                GenAiTokenTypeIncubatingValues.INPUT),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        ),
                    point -> point
                        .hasSum(response.usage().get().completionTokens())
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL,
                                response.model()),
                            equalTo(GEN_AI_TOKEN_TYPE,
                                GenAiTokenTypeIncubatingValues.COMPLETION),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        )
                )));
  }

  @Test
  void captureMessageContent() {
    List<ChatCompletionMessageParam> chatMessages = new ArrayList<>();
    chatMessages.add(createUserMessage(TEST_CHAT_INPUT));

    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .messages(chatMessages)
        .model(TEST_CHAT_MODEL)
        .build();

    long startTimeNanos = System.nanoTime();
    ChatCompletion response = client.chat().completions().create(params);
    long durationNanos = System.nanoTime() - startTimeNanos;

    String content = response.choices().get(0).message().content().get();
    assertThat(content).isNotEmpty();

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(span -> {
          assertThat(span)
              .hasAttribute(GEN_AI_OPERATION_NAME, "chat")
              .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
              .hasAttribute(GEN_AI_SYSTEM, "openai")
              .hasAttribute(GEN_AI_RESPONSE_ID, response.id())
              .hasAttribute(GEN_AI_RESPONSE_MODEL, response.model())
              .hasAttributesSatisfying(attribs -> {
                assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                    .containsExactly("stop");
              })
              .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, (long) response.usage().get().promptTokens())
              .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS,
                  (long) response.usage().get().completionTokens())
              .hasAttribute(SERVER_ADDRESS, clientHost)
              .hasAttribute(SERVER_PORT, clientPort);
        });

    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(log -> {
          assertThat(log)
              .hasAttributesSatisfying(attr -> assertThat(attr)
                  .containsEntry(GEN_AI_SYSTEM, "openai")
                  .containsEntry("event.name", "gen_ai.user.message")
              )
              .hasSpanContext(spanCtx);
          assertThat(log.getBodyValue())
              .satisfies(ValAssert.map()
                  .entry("content", TEST_CHAT_INPUT)
              );
        })
        .anySatisfy(log -> {
          assertThat(log)
              .hasAttributesSatisfying(attr -> assertThat(attr)
                  .containsEntry(GEN_AI_SYSTEM, "openai")
                  .containsEntry("event.name", "gen_ai.choice")
              )
              .hasSpanContext(spanCtx);
          assertThat(log.getBodyValue())
              .satisfies(ValAssert.map()
                  .entry("index", 0)
                  .entry("finish_reason", "stop")
                  .entry("message", ValAssert.map()
                      .entry("content", content))
              );
        });

    testing.waitAndAssertMetrics(
        "openai-client",
        metric -> metric.hasName("gen_ai.client.operation.duration")
            .hasHistogramSatisfying(
                histogram -> histogram.hasPointsSatisfying(
                    point -> point
                        .satisfies(assertThatDurationIsLessThan(durationNanos))
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL, response.model()),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        )
                )
            ),
        metric -> metric.hasName("gen_ai.client.token.usage")
            .hasHistogramSatisfying(
                histogram -> histogram.hasPointsSatisfying(
                    point -> point
                        .hasSum(response.usage().get().promptTokens())
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL, response.model()),
                            equalTo(GEN_AI_TOKEN_TYPE, GenAiTokenTypeIncubatingValues.INPUT),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        ),
                    point -> point
                        .hasSum(response.usage().get().completionTokens())
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL, response.model()),
                            equalTo(GEN_AI_TOKEN_TYPE, GenAiTokenTypeIncubatingValues.COMPLETION),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        )
                )));
  }


  @Test
  void streamWithCaptureMessageContent() throws Exception {
    List<ChatCompletionMessageParam> chatMessages = new ArrayList<>();
    chatMessages.add(createUserMessage(TEST_CHAT_INPUT));

    ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
        .messages(chatMessages)
        .model(TEST_CHAT_MODEL)
        .streamOptions(ChatCompletionStreamOptions.builder()
            .includeUsage(true)
            .build())
        .build();

    List<ChatCompletionChunk> completions;
    long startTimeNanos = System.nanoTime();
    try (StreamResponse<ChatCompletionChunk> response = client.chat().completions()
        .createStreaming(params)) {
      completions = response.stream().collect(Collectors.toList());
    }
    long durationNanos = System.nanoTime() - startTimeNanos;

    String content = completions.stream()
        .map(cc -> cc.choices().stream().findFirst().map(ChatCompletionChunk.Choice::delta)
            .orElse(null))
        .filter(Objects::nonNull)
        .map(ChatCompletionChunk.Choice.Delta::content)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.joining());
    String responseModel = completions.stream()
        .map(ChatCompletionChunk::model)
        .findFirst()
        .get();
    String responseId = completions.stream()
        .map(ChatCompletionChunk::id)
        .findFirst()
        .get();
    CompletionUsage usage = completions.stream()
        .map(ChatCompletionChunk::usage)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .get();

    assertThat(content).isNotEmpty();

    List<SpanData> spans = testing.spans();
    assertThat(spans)
        .hasSize(1)
        .first()
        .satisfies(span -> {
          assertThat(span)
              .hasAttribute(GEN_AI_SYSTEM, "openai")
              .hasAttribute(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL)
              .hasAttribute(GEN_AI_RESPONSE_MODEL, responseModel)
              .hasAttribute(GEN_AI_RESPONSE_ID, responseId)
              .hasAttribute(SERVER_ADDRESS, clientHost)
              .hasAttribute(SERVER_PORT, clientPort)
              .hasAttribute(GEN_AI_USAGE_INPUT_TOKENS, usage.promptTokens())
              .hasAttribute(GEN_AI_USAGE_OUTPUT_TOKENS, usage.completionTokens())
              .hasAttributesSatisfying(attribs -> {
                assertThat(attribs.get(GEN_AI_RESPONSE_FINISH_REASONS))
                    .containsAnyOf("stop");
              });
        });
    SpanContext spanCtx = spans.get(0).getSpanContext();
    assertThat(testing.logRecords())
        .hasSize(2)
        .anySatisfy(log -> {
          assertThat(log)
              .hasAttributesSatisfying(attr -> assertThat(attr)
                  .containsEntry("event.name", "gen_ai.user.message")
                  .containsEntry(GEN_AI_SYSTEM, "openai")
              )
              .hasSpanContext(spanCtx);
          assertThat(log.getBodyValue())
              .satisfies(ValAssert.map()
                  .entry("content", TEST_CHAT_INPUT)
              );
        })
        .anySatisfy(log -> {
          assertThat(log)
              .hasAttributesSatisfying(attr -> assertThat(attr)
                  .containsEntry("event.name", "gen_ai.choice")
                  .containsEntry(GEN_AI_SYSTEM, "openai")
              )
              .hasSpanContext(spanCtx);
          assertThat(log.getBodyValue())
              .satisfies(ValAssert.map()
                  .entry("finish_reason", "stop")
                  .entry("index", 0)
                  .entry("message", ValAssert.map()
                      .entry("content", content))
              );
        });

    testing.waitAndAssertMetrics(
        "openai-client",
        metric -> metric.hasName("gen_ai.client.operation.duration")
            .hasHistogramSatisfying(
                histogram -> histogram.hasPointsSatisfying(
                    point -> point
                        .satisfies(assertThatDurationIsLessThan(durationNanos))
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL, responseModel),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        )
                )
            ),
        metric -> metric.hasName("gen_ai.client.token.usage")
            .hasHistogramSatisfying(
                histogram -> histogram.hasPointsSatisfying(
                    point -> point
                        .hasSum(usage.promptTokens())
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL, responseModel),
                            equalTo(GEN_AI_TOKEN_TYPE, GenAiTokenTypeIncubatingValues.INPUT),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        ),
                    point -> point
                        .hasSum(usage.completionTokens())
                        .hasAttributesSatisfyingExactly(
                            equalTo(GEN_AI_SYSTEM, "openai"),
                            equalTo(GEN_AI_OPERATION_NAME, "chat"),
                            equalTo(GEN_AI_REQUEST_MODEL, TEST_CHAT_MODEL),
                            equalTo(GEN_AI_RESPONSE_MODEL, responseModel),
                            equalTo(GEN_AI_TOKEN_TYPE, GenAiTokenTypeIncubatingValues.COMPLETION),
                            equalTo(SERVER_ADDRESS, clientHost),
                            equalTo(SERVER_PORT, clientPort)
                        )
                )));
  }


  @NotNull
  private static ChatCompletionMessageParam createAssistantMessage(String content) {
    return ChatCompletionMessageParam.ofChatCompletionAssistantMessageParam(
        ChatCompletionAssistantMessageParam.builder()
            .role(ChatCompletionAssistantMessageParam.Role.ASSISTANT)
            .content(ChatCompletionAssistantMessageParam.Content.ofTextContent(content))
            .build());
  }

  @NotNull
  private static ChatCompletionMessageParam createUserMessage(String content) {
    return ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
        ChatCompletionUserMessageParam.builder()
            .role(ChatCompletionUserMessageParam.Role.USER)
            .content(ChatCompletionUserMessageParam.Content.ofTextContent(content))
            .build());
  }

  @NotNull
  private static ChatCompletionMessageParam createSystemMessage(String content) {
    return ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
        ChatCompletionSystemMessageParam.builder()
            .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
            .content(ChatCompletionSystemMessageParam.Content.ofTextContent(content))
            .build());
  }

  private static ChatCompletionMessageParam createToolMessage(String response, String id) {
    return ChatCompletionMessageParam.ofChatCompletionToolMessageParam(
        ChatCompletionToolMessageParam.builder()
            .role(ChatCompletionToolMessageParam.Role.TOOL)
            .toolCallId(id)
            .content(ChatCompletionToolMessageParam.Content.ofTextContent(response))
            .build());
  }

  private static Consumer<HistogramPointData> assertThatDurationIsLessThan(long toNanos) {
    return point ->
        assertThat(point.getSum())
            .isStrictlyBetween(0.0, (double) toNanos / TimeUnit.SECONDS.toNanos(1L));
  }
}
