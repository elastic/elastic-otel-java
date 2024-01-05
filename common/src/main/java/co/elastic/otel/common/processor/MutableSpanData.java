package co.elastic.otel.common.processor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class MutableSpanData extends DelegatingSpanData {

  @Nullable
  private Map<AttributeKey<?>, Object> attributeOverrides = null;

  private Attributes cachedMutatedAttributes = null;

  private String nameOverride = null;

  protected MutableSpanData(SpanData delegate) {
    super(delegate);
  }

  public <T> void setAttribute(AttributeKey<T> key, @Nullable T value) {
    if (attributeOverrides != null
        && attributeOverrides.containsKey(key)
        && Objects.equals(attributeOverrides.get(key), value)) {
      return;
    }
    T originalValue = super.getAttributes().get(key);
    if (Objects.equals(originalValue, value)) {
      if (attributeOverrides != null) {
        cachedMutatedAttributes = null;
        attributeOverrides.remove(key);
      }
      return;
    }
    if (attributeOverrides == null) {
      attributeOverrides = new HashMap<>();
    }
    cachedMutatedAttributes = null;
    attributeOverrides.put(key, value);
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Attributes getAttributes() {

    Attributes original = super.getAttributes();
    if (attributeOverrides == null || attributeOverrides.isEmpty()) {
      return original;
    }
    if (cachedMutatedAttributes == null) {
      AttributesBuilder attributesBuilder = Attributes.builder()
          .putAll(original);
      for (AttributeKey overrideKey : attributeOverrides.keySet()) {
        Object value = attributeOverrides.get(overrideKey);
        if (value == null) {
          attributesBuilder.remove(overrideKey);
        } else {
          attributesBuilder.put(overrideKey, value);
        }
      }
      cachedMutatedAttributes = attributesBuilder.build();
    }
    return cachedMutatedAttributes;
  }


  @SuppressWarnings("unchecked")
  public <T> T getAttribute(AttributeKey<T> key) {
    if (attributeOverrides != null && attributeOverrides.containsKey(key)) {
      return (T) attributeOverrides.get(key);
    }
    return super.getAttributes().get(key);
  }

  public void setName(String name) {
    nameOverride = name;
  }

  @Override
  public String getName() {
    if (nameOverride != null) {
      return nameOverride;
    }
    return super.getName();
  }
}
