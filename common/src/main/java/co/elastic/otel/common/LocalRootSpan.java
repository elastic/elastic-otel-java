package co.elastic.otel.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

/**
 * Shared utility for getting the local root span for a given span.
 * A local root span is a span which has no parent or a remote parent ({@link SpanContext#isRemote()}.
 * This utility is aware of wrappers (e.g. {@link MutableSpan}) and performs unwrapping if required.
 */
public class LocalRootSpan {

  private static final Object LOCAL_ROOT_MARKER = new Object();

  /**
   * Stores the local-root {@link Span} for a given Span.
   * If the span itself is a local root, it stores LOCAL_ROOT_MARKER instead:
   * Because {@link SpanValue} is backed by a weak-hash-map, letting the span
   * point to itself would cause a memory leak here.
   */
  private static final SpanValue<Object> localRoot = SpanValue.createDense();

  /**
   * Must be called at least once for every span started, otherwise {@link LocalRootSpan#getFor(Span)}
   * will not be able to provide the local root span.
   * <p/>
   * This method can safely be called multiple times for the same span
   * (e.g. from independent {@link io.opentelemetry.sdk.trace.SpanProcessor}).
   *
   * @param startedSpan the span which was just started
   * @param parentContext the context used as parent for the given span
   */
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

  /**
   * If the provided span is a local root span, itself is returned.
   * Otherwise, returns the (transitive) parent which is a local root.
   * <p>
   * NOTE: This will only work if {@link #onSpanStart(Span, Context)}
   * was called for the span and all its parents! Otherwise, null is returned.
   */
  public static Span getFor(Span span) {
    Object rootSpan = localRoot.get(span);
    return rootSpan == LOCAL_ROOT_MARKER ? span : (Span) rootSpan;
  }

  /**
   * See {@link LocalRootSpan#getFor(Span)}.
   */
  public static Span getFor(ReadWriteSpan span) {
    return getFor((ReadableSpan) span);
  }

  /**
   * See {@link LocalRootSpan#getFor(Span)}.
   */
  public static Span getFor(ReadableSpan span) {
    Object rootSpan = localRoot.get(span);
    return rootSpan == LOCAL_ROOT_MARKER ? (Span) span : (Span) rootSpan;
  }

  private LocalRootSpan() {}
}
