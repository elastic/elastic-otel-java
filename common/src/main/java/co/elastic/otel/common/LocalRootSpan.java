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
package co.elastic.otel.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import javax.annotation.Nullable;

/**
 * Shared utility for getting the local root span for a given span. A local root span is a span
 * which has no parent or a remote parent ({@link SpanContext#isRemote()}. This utility is aware of
 * wrappers (e.g. {@link MutableSpan}) and performs unwrapping if required.
 */
public class LocalRootSpan {

  private static final Object LOCAL_ROOT_MARKER = new Object();

  /**
   * Used to mark that the span or one of its parents is a delayed, inferred span. Delayed inferred
   * spans provide the parent as remote-parent, which means the actual span cannot be derived.
   */
  private static final Object INFERRED_SPAN_UNKNOWN_ROOT_MARKER = new Object();

  /**
   * Stores the local-root {@link Span} for a given Span. If the span itself is a local root, it
   * stores LOCAL_ROOT_MARKER instead: Because {@link SpanValue} is backed by a weak-hash-map,
   * letting the span point to itself would cause a memory leak here.
   */
  private static final SpanValue<Object> localRoot = SpanValue.createDense();

  /**
   * Must be called at least once for every span started, otherwise {@link
   * LocalRootSpan#getFor(Span)} will not be able to provide the local root span.
   *
   * <p>This method can safely be called multiple times for the same span (e.g. from independent
   * {@link io.opentelemetry.sdk.trace.SpanProcessor}).
   *
   * @param startedSpan the span which was just started
   * @param parentContext the context used as parent for the given span
   */
  public static void onSpanStart(ReadableSpan startedSpan, Context parentContext) {
    if (localRoot.get(startedSpan) != null) {
      // early out: this method might be called by very many SpanProcessors,
      // so it is likely good to have a fast early out check
      return;
    }
    Span parent = Span.fromContext(parentContext);
    SpanContext parentSpanCtx = parent.getSpanContext();
    if (!parentSpanCtx.isValid()) {
      localRoot.set(startedSpan, LOCAL_ROOT_MARKER);
    } else if (parentSpanCtx.isRemote()) {
      Boolean isInferred = startedSpan.getAttribute(ElasticAttributes.IS_INFERRED);
      if (isInferred != null && isInferred) {
        localRoot.set(startedSpan, INFERRED_SPAN_UNKNOWN_ROOT_MARKER);
      } else {
        localRoot.set(startedSpan, LOCAL_ROOT_MARKER);
      }
    } else {
      localRoot.set(startedSpan, getFor(parent));
    }
  }

  /** See {@link LocalRootSpan#getFor(Span)}. */
  @Nullable
  public static ReadableSpan getFor(ReadableSpan span) {
    Object rootSpanVal = localRoot.get(span);
    if (rootSpanVal == LOCAL_ROOT_MARKER) {
      return span;
    }
    if (rootSpanVal == INFERRED_SPAN_UNKNOWN_ROOT_MARKER) {
      return null;
    }
    return (ReadableSpan) rootSpanVal;
  }

  /**
   * If the provided span is a local root span, itself is returned. Otherwise, returns the
   * (transitive) parent which is a local root.
   *
   * <p>NOTE: The returned value may be null in any of the following cases:
   *
   * <ul>
   *   <li>{@link #onSpanStart(ReadableSpan, Context)} was not called for the span and all its
   *       parents
   *   <li>The provided span or one if its parents is a delayed inferred span where the parent was
   *       provided as a remote span
   * </ul>
   */
  @Nullable
  public static ReadableSpan getFor(Span span) {
    return getFor((ReadableSpan) span);
  }

  /** See {@link LocalRootSpan#getFor(Span)}. */
  @Nullable
  public static ReadableSpan getFor(ReadWriteSpan span) {
    return getFor((ReadableSpan) span);
  }

  private LocalRootSpan() {}
}
