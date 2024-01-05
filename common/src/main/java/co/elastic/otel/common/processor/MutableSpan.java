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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import javax.annotation.Nullable;

/**
 * A wrapper around an ended {@link ReadableSpan}, which allows mutation. This is done by wrapping
 * the {@link SpanData} of the provided span and returning a mutated wrapper when {@link
 * #toSpanData()} is called.
 *
 * <p>This class is not thread-safe.
 *
 * <p>Note that after {@link #toSpanData()} has been called, no more mutation are allowed. This
 * guarantees that the returned SpanData is safe to use across threads.
 */
public class MutableSpan implements ReadableSpan {

  private final ReadableSpan delegate;
  private MutableSpanData mutableSpanData;
  private SpanData cachedDelegateSpanData;

  private boolean frozen;

  private MutableSpan(ReadableSpan delegate) {
    if (!delegate.hasEnded()) {
      throw new IllegalArgumentException("The provided span has not ended yet!");
    }
    this.delegate = delegate;
  }

  /**
   * If the provided span is already mutable, it is casted and returned. Otherwise, it is wrapped in
   * a new MutableSpan instance and returned.
   *
   * @param span the span to make mutable
   */
  public static MutableSpan makeMutable(ReadableSpan span) {
    if (span instanceof MutableSpan && !((MutableSpan) span).frozen) {
      return (MutableSpan) span;
    } else {
      return new MutableSpan(span);
    }
  }

  public ReadableSpan getOriginalSpan() {
    return delegate;
  }

  private SpanData getDelegateSpanData() {
    if (cachedDelegateSpanData == null) {
      cachedDelegateSpanData = delegate.toSpanData();
    }
    return cachedDelegateSpanData;
  }

  @Override
  public SpanData toSpanData() {
    frozen = true;
    if (mutableSpanData != null) {
      return mutableSpanData;
    }
    return getDelegateSpanData();
  }

  private MutableSpanData mutate() {
    if (frozen) {
      throw new IllegalStateException(
          "toSpanData() has already been called on this span, it is no longer mutable!");
    }
    if (mutableSpanData == null) {
      mutableSpanData = new MutableSpanData(getDelegateSpanData());
    }
    return mutableSpanData;
  }

  @Nullable
  @Override
  public <T> T getAttribute(AttributeKey<T> key) {
    if (mutableSpanData != null) {
      return mutableSpanData.getAttribute(key);
    } else {
      return delegate.getAttribute(key);
    }
  }

  public <T> void removeAttribute(AttributeKey<T> key) {
    mutate().setAttribute(key, null);
  }

  public <T> void setAttribute(AttributeKey<T> key, @Nullable T value) {
    mutate().setAttribute(key, value);
  }

  @Override
  public String getName() {
    if (mutableSpanData != null) {
      return mutableSpanData.getName();
    }
    return delegate.getName();
  }

  public void setName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    mutate().setName(name);
  }

  @Override
  public SpanContext getSpanContext() {
    return delegate.getSpanContext();
  }

  @Override
  public SpanContext getParentSpanContext() {
    return delegate.getParentSpanContext();
  }

  @SuppressWarnings("deprecation")
  @Override
  public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
    return delegate.getInstrumentationLibraryInfo();
  }

  @Override
  public InstrumentationScopeInfo getInstrumentationScopeInfo() {
    return delegate.getInstrumentationScopeInfo();
  }

  @Override
  public boolean hasEnded() {
    return delegate.hasEnded();
  }

  @Override
  public long getLatencyNanos() {
    return delegate.getLatencyNanos();
  }

  @Override
  public SpanKind getKind() {
    return delegate.getKind();
  }
}
