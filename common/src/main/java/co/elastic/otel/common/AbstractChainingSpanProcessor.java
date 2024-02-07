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

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.Arrays;

/**
 * A @{@link SpanProcessor} which in addition to all standard operations is capable of modifying and
 * optionally filtering or delaying spans in the end-callback.
 *
 * <p>This is done by chaining processors and registering only the first processor with the SDK.
 * Subclasses must ensure that {@link SpanProcessor#onEnd(ReadableSpan)} is called for {@link
 * AbstractChainingSpanProcessor#next}.
 */
public abstract class AbstractChainingSpanProcessor implements SpanProcessor {

  protected final SpanProcessor next;
  private final boolean nextRequiresStart;

  /**
   * @param next the next processor to be invoked after the one being constructed.
   */
  public AbstractChainingSpanProcessor(SpanProcessor next) {
    this.next = next;
    nextRequiresStart = next.isStartRequired();
  }

  /**
   * Equivalent of {@link SpanProcessor#onStart(Context, ReadWriteSpan)}. The onStart callback of
   * the next processor must not be invoked from this method, this is already handled by the
   * implementation of {@link #onStart(Context, ReadWriteSpan)}.
   */
  protected void doOnStart(Context context, ReadWriteSpan readWriteSpan) {}

  /**
   * @return true, if this implementation would like {@link #doOnStart(Context, ReadWriteSpan)} to
   *     be invoked.
   */
  protected abstract boolean requiresStart();

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
  public final boolean isStartRequired() {
    return requiresStart() || nextRequiresStart;
  }

  @Override
  public final CompletableResultCode shutdown() {
    return CompletableResultCode.ofAll(Arrays.asList(doShutdown(), next.shutdown()));
  }

  @Override
  public final CompletableResultCode forceFlush() {
    return CompletableResultCode.ofAll(Arrays.asList(doForceFlush(), next.forceFlush()));
  }

  @Override
  public final void close() {
    SpanProcessor.super.close();
  }
}
