package io.opentelemetry.sdk.trace;

import co.elastic.otel.common.SpanValueStorage;
import co.elastic.otel.common.SpanValueStorageProvider;
import io.opentelemetry.api.trace.Span;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class FieldBackedSpanValueStorageProvider implements SpanValueStorageProvider {

  private static final String FIELD_NAME = "$elasticSpanValues";

  private static final Logger logger = Logger.getLogger(SpanValueStorage.class.getName());

  public static final FieldBackedSpanValueStorageProvider INSTANCE;

  private static final MethodHandle spanFieldGetter;
  private static final AtomicReferenceFieldUpdater<Span, Object> spanFieldSetter;

  static {
    FieldBackedSpanValueStorageProvider resultInstance = null;
    MethodHandle getter = null;
    AtomicReferenceFieldUpdater<Span, Object> setter = null;
    try {
      Field storageField = SDK_SPAN_CLASS.getDeclaredField(FIELD_NAME);

      getter = MethodHandles.lookup()
          .unreflectGetter(storageField)
          .asType(MethodType.methodType(SpanValueStorage.class, Span.class));

      setter = (AtomicReferenceFieldUpdater<Span, Object>) AtomicReferenceFieldUpdater.newUpdater(
          SDK_SPAN_CLASS, Object.class, FIELD_NAME);

      logger.log(Level.FINE, "Using field-backed storage for SpanValues", FIELD_NAME);
      resultInstance = new FieldBackedSpanValueStorageProvider();

    } catch (NoSuchFieldException e) {
      logger.log(Level.FINE,
          "Using map-backed storage for SpanValues because Field '{0}' does not exist on SdkSpan",
          FIELD_NAME);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to initialize span value storage", e);
      throw new IllegalStateException(e);
    }

    spanFieldGetter = getter;
    spanFieldSetter = setter;
    INSTANCE = resultInstance;
  }

  @Nullable
  @Override
  public SpanValueStorage get(Span span, boolean initialize) {
    SpanValueStorage value;
    try {
      value = (SpanValueStorage) spanFieldGetter.invokeExact(span);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
    if (value == null && initialize) {
      spanFieldSetter.compareAndSet(span, null, new SpanValueStorage());
      try {
        value = (SpanValueStorage) spanFieldGetter.invokeExact(span);
      } catch (Throwable e) {
        throw new IllegalStateException(e);
      }
    }
    return value;
  }
}
