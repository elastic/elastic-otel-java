package otel.extension.span;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.internal.ExtendedSpanProcessor;

public class ExampleSpanProcessor implements ExtendedSpanProcessor {

  ExampleSpanProcessor(){
  }

  @Override
  public boolean isOnEndingRequired() {
    return true;
  }

  @Override
  public void onEnding(ReadWriteSpan span) {
    // using internal method that allows to modify span when ended
    // all span attributes captured by instrumentation should be set

    // reduce known high span name cardinality, here starting with a hex string as name
    // this could be used as workaround for a high-cardinality issue in instrumentation
    if (span.getKind() == SpanKind.CLIENT && span.getName().matches("^0x[0-9a-fA-F]+")) {
      span.updateName("0x??");
    }
  }

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    // here we can only modify span attributes set on span start
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    // does not allow to modify span
  }

}
