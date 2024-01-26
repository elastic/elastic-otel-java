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

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
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
 *       space space on every span, even if the span doesn't have a value for the corresponding
 *       SpanValue. However, they are faster to access than sparse SpanValues and occupy less memory
 *       when most spans do have a value set due to not requiring a HashMap Node
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
  We attach a single AtomicReferenceArray to spans which is used as storage for ALL SpanValues.
  The first entry in this array is always a Map<SpanValue, Object>: This map is used as storage for
   * sparse SpanValues
   * dense SpanValues which have been created AFTER the AtomicReferenceArray of the given span was initialized

  Every dense SpanValue has a unique, reserved index in the AtomicReference array (starting from 1).
  They directly use the array at the corresponding index for storage.

  The AtomicReferenceArray is initialized the first time a SpanValue is written on the given span.
  It's size is the number of dense SpanValues pluse one (for the Map at index 0).

  So if the array on a span has been initialized before a given dense SpanValue, the
  dense SpanValue has no space in the array.
  In this case we simply fall back to the Map at index zero for storage.
   */

  private static final Class<?> SDK_SPAN_CLASS = getSdkSpanClass();

  // TODO: When used in agent, replace map with a field injected into SdkSpan
  // In our agent-distribution, we provide our own copy of the OTel-SDK
  // This means we could at build time add a new field to the SdkSpanClass
  // via the bytebuddy gradle plugin:
  // private volatile AtomicReferenceArray<Object> $$elasticSpanValueStore = null
  // As a result we could get entirely rid of this map in this case and just use the field for
  // storage:
  //  * Use a static final MethodHandle to read the field efficiently
  //  * Use a AtomicReferenceFieldUpdater to safely initialize the field
  // With this implementation, reading a dense SpanValue would be just a field access and a array
  // lookup!
  // In addition we wouldn't have a delayed garbage collection of values
  // like we do when using the WeakConcurrentMap
  private static final WeakConcurrentMap<Span, AtomicReferenceArray<Object>> storageMap =
      WeakConcurrent.createMap();

  // initialized with one because index zero is reserved for the map of sparse values
  private static final AtomicInteger nextDenseSpanValueIndex = new AtomicInteger(1);

  private static final int SPARSE_INDEX = Integer.MAX_VALUE;

  /**
   * The index within the AtomicReferenceArray which is reserved for this particular SpanValue. We
   * use the value {@link #SPARSE_INDEX} (=a very big int) for sparse SpanValues. This allows us to
   * avoid special cases for sparse storage: If {@link AtomicReferenceArray#length()} < index => use
   * the entry at index <br>
   * else => use the Map at index 0
   */
  private final int index;

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
    return new SpanValue<>(nextDenseSpanValueIndex.getAndIncrement());
  }

  /**
   * Creates a sparse {@link SpanValue}. Sparse SpanValues only occupy memory on Spans which
   * actually have a value set. For better performance use a dense SpanValue (see {@link
   * SpanValue#createDense()} ) if applicable.
   */
  public static <V> SpanValue<V> createSparse() {
    return new SpanValue<>(SPARSE_INDEX);
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

  @SuppressWarnings("unchecked")
  private V getImpl(Object span) {
    Span unwrapped = unwrap(span);
    AtomicReferenceArray<Object> storage = getStorage(unwrapped, false);
    if (storage != null) {
      if (storage.length() > index) {
        return (V) storage.get(index);
      } else {
        // This SpanValue is either a sparse SpanValue or the storage was allocated before
        // this dense SpanValue was registered
        // in both cases we use the map-backed mechanism at index 0
        Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(storage, false);
        if (sparseStorage != null) {
          return (V) sparseStorage.get(this);
        }
      }
    }
    return null;
  }

  private void setImpl(Object span, @Nullable V value) {
    if (value == null) {
      clearImpl(span);
      return;
    }
    Span unwrapped = unwrap(span);
    AtomicReferenceArray<Object> storage = getStorage(unwrapped, true);
    if (storage.length() > index) {
      storage.set(index, value);
    } else {
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(storage, true);
      sparseStorage.put(this, value);
    }
  }

  private boolean setIfNullImpl(Object span, @Nullable V value) {
    if (value == null) {
      return getImpl(span) == null; // setting to null if already null has no effect
    }
    Span unwrapped = unwrap(span);
    AtomicReferenceArray<Object> storage = getStorage(unwrapped, true);
    if (storage.length() > index) {
      return storage.compareAndSet(index, null, value);
    } else {
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(storage, true);
      return sparseStorage.putIfAbsent(this, value) == null;
    }
  }

  @SuppressWarnings("unchecked")
  private V computeIfNullImpl(Object span, Supplier<V> valueInitializer) {
    Span unwrapped = unwrap(span);
    AtomicReferenceArray<Object> storage = getStorage(unwrapped, true);

    if (storage.length() > index) {
      V currentValue = (V) storage.get(index);
      if (currentValue != null) {
        return currentValue;
      }
      storage.compareAndSet(index, null, valueInitializer.get());
      return (V) storage.get(index);
    } else {
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(storage, true);
      V currentValue = (V) sparseStorage.get(this);
      if (currentValue != null) {
        return currentValue;
      }
      V newValue = valueInitializer.get();
      if (newValue == null) {
        return null;
      }
      sparseStorage.putIfAbsent(this, newValue);
      return (V) sparseStorage.get(this);
    }
  }

  private void clearImpl(Object span) {
    Span unwrapped = unwrap(span);
    AtomicReferenceArray<Object> storage = getStorage(unwrapped, false);
    if (storage != null) {
      if (storage.length() > index) {
        storage.set(index, null);
      } else {
        Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(storage, false);
        if (sparseStorage != null) {
          sparseStorage.remove(this);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private static Map<SpanValue<?>, Object> getSparseValuesMap(
      AtomicReferenceArray<Object> storage, boolean initialize) {
    Map<SpanValue<?>, Object> map = (Map<SpanValue<?>, Object>) storage.get(0);
    if (map == null && initialize) {
      storage.compareAndSet(0, null, new ConcurrentHashMap<>());
      map = (Map<SpanValue<?>, Object>) storage.get(0);
    }
    return map;
  }

  @Nullable
  private static AtomicReferenceArray<Object> getStorage(Object span, boolean initialize) {
    Span unwrapped = unwrap(span);
    AtomicReferenceArray<Object> storage = storageMap.get(unwrapped);
    if (storage == null && initialize) {
      storage = new AtomicReferenceArray<>(nextDenseSpanValueIndex.get());
      storageMap.putIfAbsent(unwrapped, storage);
      storage = storageMap.get(unwrapped);
    }
    return storage;
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
}
