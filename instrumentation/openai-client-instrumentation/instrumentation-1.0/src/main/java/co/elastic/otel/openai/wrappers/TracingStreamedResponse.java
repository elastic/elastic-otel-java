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

import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletionChunk;
import com.openai.models.CompletionUsage;
import io.opentelemetry.context.Context;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TracingStreamedResponse implements StreamResponse<ChatCompletionChunk> {

  private final StreamResponse<ChatCompletionChunk> delegate;
  private final Context spanContext;
  private final InstrumentedChatCompletionService.RequestHolder requestHolder;

  private final Map<Long, String> choiceFinishReasons = new ConcurrentHashMap<>();
  private CompletionUsage usage;
  private String model;
  private String responseId;
  private boolean hasEnded = false;

  public TracingStreamedResponse(
      StreamResponse<ChatCompletionChunk> delegate,
      Context spanContext,
      InstrumentedChatCompletionService.RequestHolder requestHolder) {
    this.delegate = delegate;
    this.spanContext = spanContext;
    this.requestHolder = requestHolder;
  }

  @Override
  public Stream<ChatCompletionChunk> stream() {
    return StreamSupport.stream(new TracingSpliterator(delegate.stream().spliterator()), false);
  }

  private void collectFinishReasons(ChatCompletionChunk chunk) {
    for (ChatCompletionChunk.Choice choice : chunk.choices()) {
      Optional<ChatCompletionChunk.Choice.FinishReason> finishReason = choice.finishReason();
      if (finishReason.isPresent()) {
        choiceFinishReasons.put(choice.index(), finishReason.get().toString());
      }
    }
  }

  private synchronized void endSpan() {
    if (hasEnded) {
      return;
    }
    hasEnded = true;

    List<String> finishReasons =
        choiceFinishReasons.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());

    Long inputTokens = null;
    Long completionTokens = null;
    if (usage != null) {
      inputTokens = usage.promptTokens();
      completionTokens = usage.completionTokens();
    }

    InstrumentedChatCompletionService.ChatCompletionResult result =
        new InstrumentedChatCompletionService.ChatCompletionResult(
            model, responseId, finishReasons, inputTokens, completionTokens);
    InstrumentedChatCompletionService.INSTRUMENTER.end(spanContext, requestHolder, result, null);
  }

  @Override
  public void close() {
    endSpan();
    delegate.close();
  }

  private class TracingSpliterator implements Spliterator<ChatCompletionChunk> {

    private final Spliterator<ChatCompletionChunk> delegateSpliterator;

    private TracingSpliterator(Spliterator<ChatCompletionChunk> delegateSpliterator) {
      this.delegateSpliterator = delegateSpliterator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super ChatCompletionChunk> action) {
      boolean chunkReceived =
          delegateSpliterator.tryAdvance(
              chunk -> {
                collectFinishReasons(chunk);
                action.accept(chunk);
                String model = chunk.model();
                if (model != null && !model.isEmpty()) {
                  TracingStreamedResponse.this.model = model;
                }
                String id = chunk.id();
                if (id != null && !id.isEmpty()) {
                  TracingStreamedResponse.this.responseId = id;
                }
                chunk.usage().ifPresent(usage -> TracingStreamedResponse.this.usage = usage);
              });
      if (!chunkReceived) {
        endSpan();
      }
      return chunkReceived;
    }

    @Override
    public Spliterator<ChatCompletionChunk> trySplit() {
      // do not support parallelism to reliably catch the last chunk
      return null;
    }

    @Override
    public long estimateSize() {
      return delegateSpliterator.estimateSize();
    }

    @Override
    public long getExactSizeIfKnown() {
      return delegateSpliterator.getExactSizeIfKnown();
    }

    @Override
    public int characteristics() {
      return delegateSpliterator.characteristics();
    }

    @Override
    public Comparator<? super ChatCompletionChunk> getComparator() {
      return delegateSpliterator.getComparator();
    }
  }
}
