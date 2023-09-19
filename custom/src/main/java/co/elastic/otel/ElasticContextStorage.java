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
package co.elastic.otel;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;

public class ElasticContextStorage implements ContextStorage {

  private final ContextStorage delegate;
  private final ElasticProfiler profiler;

  ElasticContextStorage(ContextStorage delegate, ElasticProfiler profiler) {
    this.delegate = delegate;
    this.profiler = profiler;
  }

  @Override
  public Scope attach(Context toAttach) {
    // shortcut when context is already current
    if (Context.current() == toAttach) {
      return Scope.noop();
    }

    // shortcut when not recording or span already active
    Span span = Span.fromContext(toAttach);
    if (Span.current() == span || !span.isRecording()) {
      return delegate.attach(toAttach);
    }

    SpanContext spanContext = span.getSpanContext();
    SpanContext profilerPrevious = profiler.onSpanContextAttach(spanContext);
    Scope delegatedScope = delegate.attach(toAttach);
    return () -> {
      delegatedScope.close();
      profiler.onSpanContextClose(
          profilerPrevious); // profiler has to restore previously active, if any.
    };
  }

  @Override
  public Context current() {
    return delegate.current();
  }
}
