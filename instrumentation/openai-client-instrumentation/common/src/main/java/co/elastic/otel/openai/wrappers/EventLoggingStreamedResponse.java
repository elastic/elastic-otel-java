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
import io.opentelemetry.context.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class EventLoggingStreamedResponse implements StreamResponse<ChatCompletionChunk> {

  private final StreamResponse<ChatCompletionChunk> delegate;

  private final InstrumentationSettings settings;

  private final Context originalContext;

  EventLoggingStreamedResponse(
      StreamResponse<ChatCompletionChunk> delegate, InstrumentationSettings settings) {
    this.delegate = delegate;
    this.settings = settings;
    this.originalContext = Context.current();
  }

  /** Key is the choice index */
  private final Map<Long, StreamedMessageBuffer> choiceBuffers = new HashMap<>();

  @Override
  public Stream<ChatCompletionChunk> stream() {
    return delegate.stream()
        .filter(
            chunk -> {
              onChunkReceive(chunk);
              return true;
            });
  }

  private void onChunkReceive(ChatCompletionChunk completionMessage) {
    for (ChatCompletionChunk.Choice choice : completionMessage.choices()) {
      long choiceIndex = choice.index();
      StreamedMessageBuffer msg =
          choiceBuffers.computeIfAbsent(choiceIndex, _i -> new StreamedMessageBuffer());
      msg.append(choice.delta());

      if (choice.finishReason().isPresent()) {
        // message has ended, let's emit
        ChatCompletionEventsHelper.emitCompletionLogEvent(
            choiceIndex,
            choice.finishReason().get().toString(),
            msg.toEventMessageObject(settings),
            settings,
            originalContext);
        choiceBuffers.remove(choiceIndex);
      }
    }
  }

  @Override
  public void close() {
    delegate.close();
  }
}
