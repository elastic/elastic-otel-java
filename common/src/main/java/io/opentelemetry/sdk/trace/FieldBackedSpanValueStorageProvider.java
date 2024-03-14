package io.opentelemetry.sdk.trace;

import co.elastic.otel.common.SpanValueStorage;
import co.elastic.otel.common.SpanValueStorageProvider;
import io.opentelemetry.api.trace.Span;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * This class enables {@link co.elastic.otel.common.SpanValue}s to be stored directly as fields on spans.
 * The field ($elasticSpanValues) is injected at packaging time via the shadow plugin to our agent distro.
 * <p>
 * This class needs to live in the same package as the OpenTelemetry SdkSpan,
 * otherwise it is not possible to create an {@link AtomicReferenceFieldUpdater} for safely
 * initializing the field.
 */
@SuppressWarnings("unchecked")
public class FieldBackedSpanValueStorageProvider implements SpanValueStorageProvider {

  private static final String FIELD_NAME = "$elasticSpanValues";

  private static final Logger logger = Logger.getLogger(SpanValueStorage.class.getName());

  public static final FieldBackedSpanValueStorageProvider INSTANCE;

  private static final MethodHandle spanFieldGetter;
  private static final AtomicReferenceFieldUpdater<SdkSpan, Object> spanFieldSetter;

  static {
    FieldBackedSpanValueStorageProvider resultInstance = null;
    MethodHandle getter = null;
    AtomicReferenceFieldUpdater<SdkSpan, Object> setter = null;
    try {
      Field storageField = SdkSpan.class.getDeclaredField(FIELD_NAME);
      if (storageField.getType() != Object.class) {
        throw new IllegalStateException("Unexpected field type: " + storageField.getType());
      }

      getter = MethodHandles.lookup().unreflectGetter(storageField);
      setter = AtomicReferenceFieldUpdater.newUpdater(SdkSpan.class, Object.class, FIELD_NAME);

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
    SdkSpan sdkSpan = (SdkSpan) span;

    SpanValueStorage value = getFieldValue(sdkSpan);
    if (value == null && initialize) {
      spanFieldSetter.compareAndSet(sdkSpan, null, new SpanValueStorage());
      value = getFieldValue(sdkSpan);
    }
    return value;
  }

  private static SpanValueStorage getFieldValue(SdkSpan sdkSpan) {
    try {
      //double cast is required here because invokeExact is signature polymorphic
      return (SpanValueStorage) (Object) spanFieldGetter.invokeExact(sdkSpan);
    } catch (Throwable e) {
      throw new IllegalStateException(e);
    }
  }
}
