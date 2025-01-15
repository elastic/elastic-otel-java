package co.elastic.otel.openai.wrappers;

import com.openai.models.ChatCompletionChunk;
import io.opentelemetry.api.common.Value;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class StreamedMessageBuffer {

  private final StringBuilder message = new StringBuilder();

  private final Map<Long, ToolCallBuffer> toolCalls = new HashMap<>();


  public Value<?> toEventMessageObject(InstrumentationSettings settings) {
    MapValueBuilder attributes = new MapValueBuilder();
    if (settings.captureMessageContent && message.length() > 0) {
      attributes.put("content", Value.of(message.toString()));
    }
    if (!toolCalls.isEmpty()) {
      List<Value<?>> toolCallsJson = toolCalls.values().stream()
          .map(StreamedMessageBuffer::buildToolCallEventObject)
          .collect(Collectors.toList());
      attributes.put("tool_calls", Value.of(toolCallsJson));
    }
    return attributes.build();
  }

  public void append(ChatCompletionChunk.Choice.Delta delta) {
    delta.content().ifPresent(message::append);

    if (delta.toolCalls().isPresent()) {
      for (ChatCompletionChunk.Choice.Delta.ToolCall toolCall : delta.toolCalls().get()) {
        ToolCallBuffer buffer = toolCalls.computeIfAbsent(toolCall.index(),
            unused -> new ToolCallBuffer(toolCall.id().get()));
        toolCall.type().ifPresent(type -> buffer.type = type.toString());
        toolCall.function().ifPresent(function -> {
          function.name().ifPresent(name -> buffer.function.name = name);
          function.arguments().ifPresent(args -> buffer.function.arguments.append(args));
        });
      }

    }
  }

  private static Value<?> buildToolCallEventObject(ToolCallBuffer call) {
    Map<String, Value<?>> result = new HashMap<>();
    result.put("id", Value.of(call.id));
    result.put("type", Value.of(call.type));

    Map<String, Value<?>> function = new HashMap<>();
    function.put("name", Value.of(call.function.name));
    function.put("arguments", Value.of(call.function.arguments.toString()));
    result.put("function", Value.of(function));

    return Value.of(result);
  }

  private static class FunctionBuffer {
    String name;
    StringBuilder arguments = new StringBuilder();
  }

  private static class ToolCallBuffer {
    ToolCallBuffer(String id) {
      this.id = id;
    }

    final String id;
    final FunctionBuffer function = new FunctionBuffer();
    String type;
  }
}
