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

import static co.elastic.otel.openai.wrappers.GenAiAttributes.ERROR_TYPE;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_OPERATION_NAME;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_REQUEST_MODEL;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_RESPONSE_MODEL;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_SYSTEM;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_TOKEN_TYPE;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.GEN_AI_USAGE_OUTPUT_TOKENS;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.SERVER_ADDRESS;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.SERVER_PORT;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.TOKEN_TYPE_INPUT;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.TOKEN_TYPE_OUTPUT;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Defines duration and token usage metrics using histogram buckets defined here:
 * https://github.com/open-telemetry/semantic-conventions/blob/v1.29.0/docs/gen-ai/gen-ai-metrics.md
 */
final class GenAiClientMetrics implements OperationListener {

  // Visible for testing
  static final double NANOS_PER_S = TimeUnit.SECONDS.toNanos(1);

  private static final ContextKey<State> GEN_AI_CLIENT_METRICS_STATE =
      ContextKey.named("genai-client-metrics-state");

  private static final String METRIC_GEN_AI_CLIENT_OPERATION_DURATION =
      "gen_ai.client.operation.duration";
  private static final String METRIC_GEN_AI_CLIENT_TOKEN_USAGE = "gen_ai.client.token.usage";

  private static final List<Double> clientOperationBuckets = getClientOperationBuckets();
  private static final List<Long> clientTokenBuckets = getClientTokenUsageBuckets();

  private final DoubleHistogram clientOperationDuration;
  private final LongHistogram clientTokenUsage;

  GenAiClientMetrics(Meter meter) {
    clientOperationDuration =
        meter
            .histogramBuilder(METRIC_GEN_AI_CLIENT_OPERATION_DURATION)
            .setDescription("GenAI operation duration")
            .setUnit("s")
            .setExplicitBucketBoundariesAdvice(clientOperationBuckets)
            .build();
    clientTokenUsage =
        meter
            .histogramBuilder(METRIC_GEN_AI_CLIENT_TOKEN_USAGE)
            .ofLongs()
            .setDescription("Measures number of input and output tokens used")
            .setUnit("{token}")
            .setExplicitBucketBoundariesAdvice(clientTokenBuckets)
            .build();
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(GEN_AI_CLIENT_METRICS_STATE, new State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(GEN_AI_CLIENT_METRICS_STATE);
    if (state == null) {
      return;
    }
    AttributesBuilder commonBuilder = Attributes.builder();
    copyAttribute(state.startAttributes, commonBuilder, SERVER_PORT);
    copyAttribute(state.startAttributes, commonBuilder, SERVER_ADDRESS);
    copyAttribute(state.startAttributes, commonBuilder, GEN_AI_OPERATION_NAME);
    copyAttribute(state.startAttributes, commonBuilder, GEN_AI_SYSTEM);
    copyAttribute(state.startAttributes, commonBuilder, GEN_AI_REQUEST_MODEL);
    copyAttribute(endAttributes, commonBuilder, GEN_AI_RESPONSE_MODEL);
    Attributes common = commonBuilder.build();

    AttributesBuilder durationAttributesBuilder = common.toBuilder();
    copyAttribute(endAttributes, durationAttributesBuilder, ERROR_TYPE);
    clientOperationDuration.record(
        (endNanos - state.startTimeNanos) / NANOS_PER_S,
        durationAttributesBuilder.build(),
        context);

    Long inputTokens = endAttributes.get(GEN_AI_USAGE_INPUT_TOKENS);
    if (inputTokens != null) {
      clientTokenUsage.record(
          inputTokens,
          common.toBuilder().put(GEN_AI_TOKEN_TYPE, TOKEN_TYPE_INPUT).build(),
          context);
    }
    Long outputTokens = endAttributes.get(GEN_AI_USAGE_OUTPUT_TOKENS);
    if (outputTokens != null) {
      clientTokenUsage.record(
          outputTokens,
          common.toBuilder().put(GEN_AI_TOKEN_TYPE, TOKEN_TYPE_OUTPUT).build(),
          context);
    }
  }

  private static <T> void copyAttribute(
      Attributes from, AttributesBuilder to, AttributeKey<T> key) {
    T value = from.get(key);
    if (value != null) {
      to.put(key, value);
    }
  }

  private static class State {
    final Attributes startAttributes;
    final long startTimeNanos;

    private State(Attributes startAttributes, long startTimeNanos) {
      this.startAttributes = startAttributes;
      this.startTimeNanos = startTimeNanos;
    }
  }

  private static List<Double> getClientOperationBuckets() {
    List<Double> clientOperationBuckets = new ArrayList<>();
    clientOperationBuckets.add(0.01);
    clientOperationBuckets.add(0.02);
    clientOperationBuckets.add(0.04);
    clientOperationBuckets.add(0.08);
    clientOperationBuckets.add(0.16);
    clientOperationBuckets.add(0.32);
    clientOperationBuckets.add(0.64);
    clientOperationBuckets.add(1.28);
    clientOperationBuckets.add(2.56);
    clientOperationBuckets.add(5.12);
    clientOperationBuckets.add(10.24);
    clientOperationBuckets.add(20.48);
    clientOperationBuckets.add(40.96);
    clientOperationBuckets.add(81.92);
    return Collections.unmodifiableList(clientOperationBuckets);
  }

  private static List<Long> getClientTokenUsageBuckets() {
    List<Long> clientTokenUsageBuckets = new ArrayList<>();
    clientTokenUsageBuckets.add(1L);
    clientTokenUsageBuckets.add(4L);
    clientTokenUsageBuckets.add(16L);
    clientTokenUsageBuckets.add(64L);
    clientTokenUsageBuckets.add(256L);
    clientTokenUsageBuckets.add(1024L);
    clientTokenUsageBuckets.add(4096L);
    clientTokenUsageBuckets.add(16384L);
    clientTokenUsageBuckets.add(65536L);
    clientTokenUsageBuckets.add(262144L);
    clientTokenUsageBuckets.add(1048576L);
    clientTokenUsageBuckets.add(4194304L);
    clientTokenUsageBuckets.add(16777216L);
    clientTokenUsageBuckets.add(67108864L);
    return Collections.unmodifiableList(clientTokenUsageBuckets);
  }
}
