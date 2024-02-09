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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;
import javax.annotation.Nullable;

// We use inheritance over composition here to not
// waste space and time with an additional reference and object
class SpanValueStorage extends AtomicReferenceArray<Object> {

  // initialized with one because index zero is reserved for the map of sparse values
  private static final AtomicInteger nextDenseSpanValueIndex = new AtomicInteger(1);

  private static final int SPARSE_INDEX = Integer.MAX_VALUE;

  SpanValueStorage() {
    super(nextDenseSpanValueIndex.get());
  }

  static int allocateDenseIndex() {
    return nextDenseSpanValueIndex.getAndIncrement();
  }

  static int allocateSparseIndex() {
    return SPARSE_INDEX;
  }

  @SuppressWarnings("unchecked")
  <V> V get(SpanValue<V> key) {
    if (length() > key.index) {
      return (V) get(key.index);
    } else {
      // The provided SpanValue is either a sparse SpanValue or the storage was allocated before
      // the dense SpanValue was registered
      // in both cases we use the map-backed mechanism at index 0
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(false);
      if (sparseStorage != null) {
        return (V) sparseStorage.get(key);
      }
    }
    return null;
  }

  <V> void set(SpanValue<V> key, V value) {
    Objects.requireNonNull(value);
    if (length() > key.index) {
      set(key.index, value);
    } else {
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(true);
      sparseStorage.put(key, value);
    }
  }

  <V> boolean setIfNull(SpanValue<V> key, V value) {
    Objects.requireNonNull(value);
    if (length() > key.index) {
      return compareAndSet(key.index, null, value);
    } else {
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(true);
      return sparseStorage.putIfAbsent(key, value) == null;
    }
  }

  @SuppressWarnings("unchecked")
  <V> V computeIfNull(SpanValue<V> key, Supplier<V> valueInitializer) {
    int index = key.index;
    if (length() > index) {
      V currentValue = (V) get(index);
      if (currentValue != null) {
        return currentValue;
      }
      compareAndSet(index, null, valueInitializer.get());
      return (V) get(index);
    } else {
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(true);
      V currentValue = (V) sparseStorage.get(key);
      if (currentValue != null) {
        return currentValue;
      }
      V newValue = valueInitializer.get();
      if (newValue == null) {
        return null;
      }
      sparseStorage.putIfAbsent(key, newValue);
      return (V) sparseStorage.get(key);
    }
  }

  void clear(SpanValue<?> key) {
    if (length() > key.index) {
      set(key.index, null);
    } else {
      Map<SpanValue<?>, Object> sparseStorage = getSparseValuesMap(false);
      if (sparseStorage != null) {
        sparseStorage.remove(key);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Nullable
  private Map<SpanValue<?>, Object> getSparseValuesMap(boolean initialize) {
    Map<SpanValue<?>, Object> map = (Map<SpanValue<?>, Object>) get(0);
    if (map == null && initialize) {
      compareAndSet(0, null, new ConcurrentHashMap<>());
      map = (Map<SpanValue<?>, Object>) get(0);
    }
    return map;
  }
}
