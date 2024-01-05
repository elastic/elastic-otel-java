package co.elastic.otel.common.processor;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Arrays;

/**
 * A @{@link io.opentelemetry.sdk.trace.SpanProcessor}
 * which in addition to all standard operations is capable
 * of modifying and optionally filtering spans in the end-callback.
 */
public abstract class AbstractSimpleChainingSpanProcessor implements SpanProcessor {

  protected final SpanProcessor next;
  private final boolean nextRequiresStart;
  private final boolean nextRequiresEnd;

  public AbstractSimpleChainingSpanProcessor(SpanProcessor next) {
    this.next = next;
    nextRequiresStart = next.isStartRequired();
    nextRequiresEnd = next.isEndRequired();
  }


  protected void doOnStart(Context context, ReadWriteSpan readWriteSpan) {
  }

  protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
    return readableSpan;
  }

  protected boolean requiresStart() {
    return true;
  }

  protected boolean requiresEnd() {
    return true;
  }

  protected CompletableResultCode doForceFlush() {
    return CompletableResultCode.ofSuccess();
  }

  protected CompletableResultCode doShutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public final void onStart(Context context, ReadWriteSpan readWriteSpan) {
    try {
      if (requiresStart()) {
        doOnStart(context, readWriteSpan);
      }
    } finally {
      if (nextRequiresStart) {
        next.onStart(context, readWriteSpan);
      }
    }
  }

  @Override
  public final void onEnd(ReadableSpan readableSpan) {
    ReadableSpan mappedTo = readableSpan;
    try {
      if (requiresEnd()) {
        mappedTo = doOnEnd(readableSpan);
      }
    } finally {
      if (mappedTo != null && nextRequiresEnd) {
        next.onEnd(mappedTo);
      }
    }
  }

  @Override
  public final boolean isStartRequired() {
    return requiresStart() || nextRequiresStart;
  }

  @Override
  public final boolean isEndRequired() {
    return requiresEnd() || nextRequiresEnd;
  }

  @Override
  public final CompletableResultCode shutdown() {
    return CompletableResultCode.ofAll(Arrays.asList(
        doShutdown(),
        next.shutdown()
    ));
  }

  @Override
  public final CompletableResultCode forceFlush() {
    return CompletableResultCode.ofAll(Arrays.asList(
        doForceFlush(),
        next.forceFlush()
    ));
  }

  @Override
  public final void close() {
    SpanProcessor.super.close();
  }
}
