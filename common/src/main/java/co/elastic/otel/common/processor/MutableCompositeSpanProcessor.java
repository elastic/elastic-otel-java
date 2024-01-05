package co.elastic.otel.common.processor;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.ArrayList;
import java.util.List;

class MutableCompositeSpanProcessor implements SpanProcessor {

  private final List<SpanProcessor> delegates = new ArrayList<>();

  //visibile for testing
  SpanProcessor composite = SpanProcessor.composite();

  public boolean isEmpty() {
    return delegates.isEmpty();
  }

  public void addDelegate(SpanProcessor processor) {
    delegates.add(processor);
    composite = SpanProcessor.composite(delegates);
  }

  @Override
  public CompletableResultCode shutdown() {
    return composite.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return composite.forceFlush();
  }

  @Override
  public void close() {
    composite.close();
  }

  @Override
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {
    composite.onStart(context, readWriteSpan);
  }

  @Override
  public boolean isStartRequired() {
    return composite.isStartRequired();
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {
    composite.onEnd(readableSpan);
  }

  @Override
  public boolean isEndRequired() {
    return composite.isEndRequired();
  }
}
