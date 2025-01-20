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
package co.elastic.otel.openai.wrappers;

import io.opentelemetry.api.common.Value;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builder for fluently building a map-valued {@link Value}. */
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
