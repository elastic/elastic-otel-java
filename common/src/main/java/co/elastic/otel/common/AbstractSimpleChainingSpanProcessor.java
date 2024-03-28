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

import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Mutations can be performed in {@link #doOnEnd(ReadableSpan)} by wrapping the span in a {@link
 * MutableSpan}
 */
public abstract class AbstractSimpleChainingSpanProcessor extends AbstractChainingSpanProcessor {
  private final boolean nextRequiresEnd;

  /**
   * @param next the next processor to be invoked after the one being constructed.
   */
  public AbstractSimpleChainingSpanProcessor(SpanProcessor next) {
    super(next);
    nextRequiresEnd = next.isEndRequired();
  }

  /**
   * Equivalent of {@link SpanProcessor#onEnd(ReadableSpan)}}.
   *
   * <p>If this method returns null, the provided span will be dropped and not passed to the next
   * processor. If anything non-null is returned, the returned instance is passed to the next
   * processor.
   *
   * <p>So in order to mutate the span, simply use {@link MutableSpan#makeMutable(ReadableSpan)} on
   * the provided argument and return the {@link MutableSpan} from this method.
   */
  protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
    return readableSpan;
  }

  /**
   * @return true, if this implementation would like {@link #doOnEnd(ReadableSpan)} to be invoked.
   */
  protected abstract boolean requiresEnd();

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
  public final boolean isEndRequired() {
    return requiresEnd() || nextRequiresEnd;
  }
}
