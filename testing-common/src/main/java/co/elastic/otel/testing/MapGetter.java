package co.elastic.otel.testing;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Map;
import javax.annotation.Nullable;

public class MapGetter implements TextMapGetter<Map<String, String>> {
  @Override
  public Iterable<String> keys(Map<String, String> map) {
    return map.keySet();
  }

  @Nullable
  @Override
  public String get(Map<String, String> map, String key) {
    return map.get(key);
  }
}
