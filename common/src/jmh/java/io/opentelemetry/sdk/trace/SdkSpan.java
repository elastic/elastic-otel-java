package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import java.util.concurrent.TimeUnit;

public class SdkSpan implements Span {

  public Object normalField;

  volatile Object $elasticSpanValues;

  @Override
  public <T> Span setAttribute(AttributeKey<T> attributeKey, T t) {
    return null;
  }

  @Override
  public Span addEvent(String s, Attributes attributes) {
    return null;
  }

  @Override
  public Span addEvent(String s, Attributes attributes, long l, TimeUnit timeUnit) {
    return null;
  }

  @Override
  public Span setStatus(StatusCode statusCode, String s) {
    return null;
  }

  @Override
  public Span recordException(Throwable throwable, Attributes attributes) {
    return null;
  }

  @Override
  public Span updateName(String s) {
    return null;
  }

  @Override
  public void end() {

  }

  @Override
  public void end(long l, TimeUnit timeUnit) {

  }

  @Override
  public SpanContext getSpanContext() {
    return null;
  }

  @Override
  public boolean isRecording() {
    return false;
  }
}
