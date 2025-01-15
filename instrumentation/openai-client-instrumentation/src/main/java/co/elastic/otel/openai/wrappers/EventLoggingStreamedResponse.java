package co.elastic.otel.openai.wrappers;

import com.openai.core.http.StreamResponse;
import com.openai.models.ChatCompletionChunk;
import io.opentelemetry.context.Context;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class EventLoggingStreamedResponse implements StreamResponse<ChatCompletionChunk> {

    private final StreamResponse<ChatCompletionChunk> delegate;

    private final InstrumentationSettings settings;

    private final Context originalContext;

    EventLoggingStreamedResponse(StreamResponse<ChatCompletionChunk> delegate, InstrumentationSettings settings) {
        this.delegate = delegate;
        this.settings = settings;
        this.originalContext = Context.current();
    }

    /**
     * Key is the choice index
     */
    private final Map<Long, StreamedMessageBuffer> choiceBuffers = new HashMap<>();

    @NotNull
    @Override
    public Stream<ChatCompletionChunk> stream() {
        return delegate.stream().filter(chunk -> {
            onChunkReceive(chunk);
            return true;
        });
    }

    private void onChunkReceive(ChatCompletionChunk completionMessage) {
        for (ChatCompletionChunk.Choice choice : completionMessage.choices()) {
            long choiceIndex = choice.index();
            StreamedMessageBuffer msg = choiceBuffers.computeIfAbsent(choiceIndex, _i -> new StreamedMessageBuffer());
            msg.append(choice.delta());

            if(choice.finishReason().isPresent()) {
                //message has ended, let's emit
                ChatCompletionEventsHelper.emitCompletionLogEvent(choiceIndex, choice.finishReason().get().toString(), msg.toEventMessageObject(settings), settings, originalContext);
                choiceBuffers.remove(choiceIndex);
            }

        }
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
