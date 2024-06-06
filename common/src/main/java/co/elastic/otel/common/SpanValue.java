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
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Utility for attaching any kind of objects to spans. Like a {@link ThreadLocal} attaches values to
 * threads, a {@link SpanValue} attaches values to {@link Span}s.
 *
 * <p>{@link SpanValue}s are thread safe and offer atomic update operations (see {@link
 * SpanValue#setIfNull(Span, Object)} and {@link SpanValue#computeIfNull(Span, Supplier)}).
 *
 * <p>{@link SpanValue}s are aware of wrappers such as {@link MutableSpan}: Writing a {@link
 * SpanValue} on a {@link ReadableSpan} and reading it on a {@link MutableSpan} wrapping the
 * original {@link ReadableSpan} will yield the originally written value (and vice-versa).
 *
 * <p>There are two variants of {@link SpanValue}s: dense and sparse
 *
 * <ul>
 *   <li>dense SpanValues are stored as array entries in a backing array: This means they occupy
 *       space on every span, even if the span doesn't have a value for the corresponding SpanValue.
 *       However, they are faster to access than sparse SpanValues and occupy less memory when most
 *       spans do have a value set due to not requiring a HashMap Node
 *   <li>sparse SpanValues are stored as entries in a backing Map: They don't occupy space on spans
 *       which do not have a value for them, but are slower to access and require a HashMap Node
 *       when stored.
 * </ul>
 *
 * <p>NOTE: the stored values must not have strong reference to the spans they are attached to, as
 * this will cause a memory leak!
 *
 * @param <V> the type of the value to be attached to the spans
 */
public class SpanValue<V> {
  /*
  IMPLEMENTATION NOTES
  We attach a single AtomicReferenceArray (implemented via SpanValueStorage) to spans which is used
  as storage for ALL SpanValues.
  The first entry in this array is always a Map<SpanValue, Object>: This map is used as storage for
   * sparse SpanValues
   * dense SpanValues which have been created AFTER the AtomicReferenceArray of the given span was initialized

  Every dense SpanValue has a unique, reserved index in the AtomicReference array (starting from 1).
  They directly use the array at the corresponding index for storage.

  The AtomicReferenceArray is initialized the first time a SpanValue is written on the given span.
  Its size is the number of dense SpanValues plus one (for the Map at index 0).

  So if the array on a span has been initialized before a given dense SpanValue, the
  dense SpanValue has no space in the array.
  In this case we simply fall back to the Map at index zero for storage.
   */

  private static final Class<?> SDK_SPAN_CLASS = getSdkSpanClass();
  private static final Class<?> CONTRIB_MUTABLE_SPAN_CLASS = getContribMutableSpanClass();

  private static final SpanValueStorageProvider storageProvider = SpanValueStorageProvider.get();

  /**
   * The index within the {@link SpanValueStorage} which is reserved for this particular SpanValue.
   * We use the {@link Integer#MAX_VALUE} for sparse SpanValues. This allows us to avoid special
   * cases for sparse storage: If {@link AtomicReferenceArray#length()} < index => use the entry at
   * index <br>
   * else => use the Map at index 0
   */
  final int index;

  private SpanValue(int index) {
    this.index = index;
  }

  /**
   * Create a dense {@link SpanValue}. Every instance of a dense SpanValue will require space on a
   * Span, independent of whether the span has a value for it or not!
   *
   * <p>For this reason a dense SpanValue should almost always be stored in a {@code static final}
   * field.
   *
   * <p>Dense SpanValues are faster to access. They occupy less memory than sparse ones if the value
   * is mostly populated.
   *
   * <p>Should always be used if roughly 20% or more of all spans will have a value for this
   * SpanValue.
   *
   * <p>See {@link SpanValue#createSparse()}
   */
  public static <V> SpanValue<V> createDense() {
    return new SpanValue<>(SpanValueStorage.allocateDenseIndex());
  }

  /**
   * Creates a sparse {@link SpanValue}. Sparse SpanValues only occupy memory on Spans which
   * actually have a value set. For better performance use a dense SpanValue (see {@link
   * SpanValue#createDense()} ) if applicable.
   */
  public static <V> SpanValue<V> createSparse() {
    return new SpanValue<>(SpanValueStorage.allocateSparseIndex());
  }

  /** Reads the current value for the given span. */
  @Nullable
  public V get(Span span) {
    return getImpl(span);
  }

  /** See {@link SpanValue#get(Span)}. */
  @Nullable
  public V get(ReadableSpan span) {
    return getImpl(span);
  }

  /** See {@link SpanValue#get(Span)}. */
  @Nullable
  public V get(ReadWriteSpan span) {
    return getImpl(span);
  }

  /**
   * Sets the value for the given span.
   *
   * @param span the span to attach the value to
   * @param value the value to set. If null, has the same effect as calling {@link
   *     SpanValue#clear(Span)}
   */
  public void set(Span span, @Nullable V value) {
    setImpl(span, value);
  }

  /** See {@link SpanValue#set(Span, Object)}. */
  public void set(ReadableSpan span, @Nullable V value) {
    setImpl(span, value);
  }

  /** See {@link SpanValue#set(Span, Object)}. */
  public void set(ReadWriteSpan span, @Nullable V value) {
    setImpl(span, value);
  }

  /**
   * Same as {@link SpanValue#set(Span, Object)}, but only atomically performs the set if the
   * current value for the given span is null.
   *
   * @param span the span to attach to
   * @param value the value to set
   * @return true, if the value was set. false if the span already had a non-null value attached
   */
  public boolean setIfNull(Span span, @Nullable V value) {
    return setIfNullImpl(span, value);
  }

  /** See {@link SpanValue#setIfNull(Span, Object)}. */
  public boolean setIfNull(ReadableSpan span, @Nullable V value) {
    return setIfNullImpl(span, value);
  }

  /** See {@link SpanValue#setIfNull(Span, Object)}. */
  public boolean setIfNull(ReadWriteSpan span, @Nullable V value) {
    return setIfNullImpl(span, value);
  }

  /**
   * Same as {@link SpanValue#setIfNull(Span, Object)}, but lazily fetched the value from the
   * provided {@link Supplier} if required.
   *
   * @param span the span to attach the value to
   * @param valueInitializer provides the value to attach, should be side effect free
   * @return the value returned by valueInitializer if an initialization was performed, otherwise
   *     returns the already attached non-null value
   */
  public V computeIfNull(Span span, Supplier<V> valueInitializer) {
    return computeIfNullImpl(span, valueInitializer);
  }

  /** See {@link SpanValue#computeIfNull(Span, Supplier)}. */
  public V computeIfNull(ReadableSpan span, Supplier<V> valueInitializer) {
    return computeIfNullImpl(span, valueInitializer);
  }

  /** See {@link SpanValue#computeIfNull(Span, Supplier)}. */
  public V computeIfNull(ReadWriteSpan span, Supplier<V> valueInitializer) {
    return computeIfNullImpl(span, valueInitializer);
  }

  /** Removes the attached value for the given span, if a non-null value was attached. */
  public void clear(Span span) {
    clearImpl(span);
  }

  /** See {@link SpanValue#clear(Span)}. */
  public void clear(ReadableSpan span) {
    clearImpl(span);
  }

  /** See {@link SpanValue#clear(Span)}. */
  public void clear(ReadWriteSpan span) {
    clearImpl(span);
  }

  private V getImpl(Object span) {
    SpanValueStorage storage = getStorage(span, false);
    if (storage != null) {
      return storage.get(this);
    }
    return null;
  }

  private void setImpl(Object span, @Nullable V value) {
    if (value == null) {
      clearImpl(span);
      return;
    }
    SpanValueStorage storage = getStorage(span, true);
    storage.set(this, value);
  }

  private boolean setIfNullImpl(Object span, @Nullable V value) {
    if (value == null) {
      return getImpl(span) == null; // setting to null if already null has no effect
    }
    SpanValueStorage storage = getStorage(span, true);
    return storage.setIfNull(this, value);
  }

  private V computeIfNullImpl(Object span, Supplier<V> valueInitializer) {
    SpanValueStorage storage = getStorage(span, true);
    return storage.computeIfNull(this, valueInitializer);
  }

  private void clearImpl(Object span) {
    SpanValueStorage storage = getStorage(span, false);
    if (storage != null) {
      storage.clear(this);
    }
  }

  @Nullable
  private static SpanValueStorage getStorage(Object span, boolean initialize) {
    Span unwrapped = unwrap(span);
    return storageProvider.get(unwrapped, initialize);
  }

  /** Provides the underlying {@link SdkSpan} instance in case the given span is wrapped. */
  private static Span unwrap(Object span) {
    if (span.getClass() == SDK_SPAN_CLASS) {
      if (!((Span) span).getSpanContext().isValid()) {
        throw new IllegalArgumentException("SpanValues don't work with invalid spans!");
      }
      return (Span) span;
    }
    if (span instanceof MutableSpan) {
      return unwrap(((MutableSpan) span).getOriginalSpan());
    }
    if (CONTRIB_MUTABLE_SPAN_CLASS != null && CONTRIB_MUTABLE_SPAN_CLASS.isInstance(span)) {
      return unwrap(ContribMutableSpanAccessor.getOriginalSpan(span));
    }
    if (span instanceof Span && !((Span) span).getSpanContext().isValid()) {
      throw new IllegalArgumentException("SpanValues don't work with invalid spans!");
    }

    throw new IllegalStateException("unknown span type: " + span.getClass().getName());
  }

  private static Class<?> getSdkSpanClass() {
    try {
      return Class.forName("io.opentelemetry.sdk.trace.SdkSpan");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Expected class to exist", e);
    }
  }

  @Nullable
  private static Class<?> getContribMutableSpanClass() {
    try {
      return Class.forName("io.opentelemetry.contrib.stacktrace.internal.MutableSpan");
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static class ContribMutableSpanAccessor {
    public static ReadableSpan getOriginalSpan(Object span) {
      return ((io.opentelemetry.contrib.stacktrace.internal.MutableSpan) span).getOriginalSpan();
    }
  }
}
