package co.elastic.otel;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.openjdk.jmh.infra.Blackhole;

public class BlackholeSpanProcessor implements SpanProcessor {

  private final Blackhole blackhole;

  public BlackholeSpanProcessor(Blackhole blackhole) {
    this.blackhole = blackhole;
  }

  @Override
  public void onStart(Context context, ReadWriteSpan readWriteSpan) {

  }

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan readableSpan) {
    blackhole.consume(readableSpan);
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
