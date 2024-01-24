package co.elastic.otel.common;

import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.Nullable;

public class SpanValue<V> {

  private static final Class<?> SDK_SPAN_CLASS = getSdkSpanClass();


  private static final WeakConcurrentMap<Span, AtomicReferenceArray<Object>> storageMap
      = WeakConcurrent.createMap();

  //initialized with one because index zero is reserved for the map of sparse values
  private static final AtomicInteger nextDenseSpanValueIndex = new AtomicInteger(1);

  private static final int SPARSE_INDEX = Integer.MAX_VALUE;

  private int index;

  private SpanValue(int index) {
    this.index = index;
  }

  public static <V> SpanValue<V> createDense() {
    return new SpanValue<>(nextDenseSpanValueIndex.getAndIncrement());
  }

  public static <V> SpanValue<V> createSparse() {
    return new SpanValue<>(SPARSE_INDEX);
  }

  @Nullable
  public V get(Span span) {
    return getImpl(span);
  }

  @Nullable
  public V get(ReadableSpan span) {
    return getImpl(span);
  }

  public void set(Span span, @Nullable V value) {
    setImpl(span, value);
  }

  public void set(ReadableSpan span, @Nullable V value) {
    setImpl(span, value);
  }

  public boolean setIfNull(Span span, @Nullable V value) {
    return setIfNullImpl(span, value);
  }

  public boolean setIfNull(ReadableSpan span, @Nullable V value) {
    return setIfNullImpl(span, value);
  }

  public void clear(Span span) {
    clearImpl(span);
  }

  public void clear(ReadableSpan span) {
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

  private boolean setIfNullImpl(Object span, @Nullable V value) {
    if (value == null) {
      return getImpl(span) == null; //setting to null if already null has no effect
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
  private static Map<SpanValue<?>, Object> getSparseValuesMap(AtomicReferenceArray<Object> storage,
      boolean initialize) {
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

  /**
   * Provides the underlying {@link SdkSpan} instance in case the given span is wrapped.
   */
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
