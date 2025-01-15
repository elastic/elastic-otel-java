package co.elastic.otel.openai.wrappers;

import io.opentelemetry.api.common.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for fluently building a map-valued {@link Value}.
 */
public class MapValueBuilder {

    private final Map<String, Value<?>> entries = new LinkedHashMap<>();

    public MapValueBuilder put(String key, Value<?> val) {
        entries.put(key, val);
        return this;
    }

    public <T> MapValueBuilder put(String key, String val) {
        entries.put(key, Value.of(val));
        return this;
    }

    public <T> MapValueBuilder put(String key, long val) {
        entries.put(key, Value.of(val));
        return this;
    }

    public Value<?> build() {
        return Value.of(entries);
    }
}
