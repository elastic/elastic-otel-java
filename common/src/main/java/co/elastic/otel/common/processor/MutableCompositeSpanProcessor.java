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

  // visibile for testing
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
