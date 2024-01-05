package co.elastic.otel.common.processor;

import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Function;

public interface ChainingSpanProcessorRegisterer {

  int ORDER_FIRST = Integer.MIN_VALUE;
  int ORDER_DEFAULT = 0;
  int ORDER_LAST = Integer.MAX_VALUE;

  default void register(Function<SpanProcessor, SpanProcessor> processorFactory) {
    register(processorFactory, ORDER_DEFAULT);
  }

  void register(Function<SpanProcessor, SpanProcessor> processorFactory, int order);
}
