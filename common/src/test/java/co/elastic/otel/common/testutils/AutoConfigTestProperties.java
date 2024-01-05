package co.elastic.otel.common.testutils;

import javax.annotation.Nullable;

public class AutoConfigTestProperties extends TemporaryProperties {

  public AutoConfigTestProperties() {
    put("otel.java.global-autoconfigure.enabled", "true");
    put("otel.traces.exporter", "logging");
    put("otel.metrics.exporter", "logging");
    put("otel.logs.exporter", "logging");
  }

  @Override
  public AutoConfigTestProperties put(String key, @Nullable String value) {
    super.put(key, value);
    return this;
  }
}

