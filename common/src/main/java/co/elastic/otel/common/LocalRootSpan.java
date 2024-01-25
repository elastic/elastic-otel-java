package co.elastic.otel.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class LocalRootSpan {

  private static final Object LOCAL_ROOT_MARKER = new Object();

  /**
   * Stores the local-root {@link Span} for a given Span.
   * If the span itself is a local root, it stores LOCAL_ROOT_MARKER instead:
   * Because {@link SpanValue} is backed by a weak-hash-map, letting the span
   * point to itself would cause a memory leak here.
   */
  private static final SpanValue<Object> localRoot = SpanValue.createDense();

  public static void onSpanStart(Span startedSpan, Context parentContext) {
    if (localRoot.get(startedSpan) != null) {
      // early out: this method might be called by very many SpanProcessors,
      // so it is likely good to have a fast early out check
      return;
    }
    Span parent = Span.fromContext(parentContext);
    SpanContext parentSpanCtx = parent.getSpanContext();
    if (!parentSpanCtx.isValid() || parentSpanCtx.isRemote()) {
      localRoot.set(startedSpan, LOCAL_ROOT_MARKER);
    } else {
      localRoot.set(startedSpan, getFor(parent));
    }
  }

  public static Span getFor(Span span) {
    Object rootSpan = localRoot.get(span);
    return rootSpan == LOCAL_ROOT_MARKER ? span : (Span) rootSpan;
  }

  public static Span getFor(ReadWriteSpan span) {
    return getFor((ReadableSpan) span);
  }

  public static Span getFor(ReadableSpan span) {
    Object rootSpan = localRoot.get(span);
    return rootSpan == LOCAL_ROOT_MARKER ? (Span) span : (Span) rootSpan;
  }

  private LocalRootSpan() {}
}
