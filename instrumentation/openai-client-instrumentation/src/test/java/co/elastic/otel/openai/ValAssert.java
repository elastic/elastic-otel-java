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
package co.elastic.otel.openai;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.common.ValueType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ValAssert extends Consumer<Value<?>>, BiConsumer<String, Value<?>> {

  default void accept(Value<?> value) {
    accept(null, value);
  }

  static ForMap map() {
    return new ForMap();
  }

  static ForArray array() {
    return new ForArray();
  }

  static ValAssert of(String value) {
    return (path, val) -> {
      assertThat(val.getType())
          .describedAs(descriptionForPath(path, "to have a string value"))
          .isEqualTo(ValueType.STRING);
      assertThat((String) val.getValue())
          .describedAs(descriptionForPath(path, "to match"))
          .isEqualTo(value);
    };
  }

  static ValAssert of(boolean value) {
    return (path, val) -> {
      assertThat(val.getType())
          .describedAs(descriptionForPath(path, "to have a boolean value"))
          .isEqualTo(ValueType.BOOLEAN);
      assertThat((boolean) val.getValue())
          .describedAs(descriptionForPath(path, "to match"))
          .isEqualTo(value);
    };
  }

  static ValAssert of(long value) {
    return (path, val) -> {
      assertThat(val.getType())
          .describedAs(descriptionForPath(path, "to have a integer value"))
          .isEqualTo(ValueType.LONG);
      assertThat((Long) val.getValue())
          .describedAs(descriptionForPath(path, "to match"))
          .isEqualTo(value);
    };
  }

  static ValAssert of(Object value) {
    Class<?> valClass = value.getClass();
    if (valClass == Boolean.class) {
      return of(((Boolean) value).booleanValue());
    }
    if (valClass == Long.class) {
      return of(((Long) value).longValue());
    }
    if (valClass == Integer.class) {
      return of(((Integer) value).longValue());
    }
    if (valClass == String.class) {
      return of(((String) value));
    }
    if (valClass.isArray()) {
      Object[] arr = (Object[]) value;
      ForArray result = array();
      for (Object entry : arr) {
        result.entry(of(entry));
      }
      return result;
    }
    throw new IllegalArgumentException("Unhandled type: " + valClass.getName());
  }

  static String descriptionForPath(String path, String check) {
    if (path == null) {
      return "Expected AnyValue " + check;
    } else {
      return "Expected AnyValue under path '" + path + "' " + check;
    }
  }

  class ForMap implements ValAssert {

    private final Map<String, ValAssert> expectedElements = new LinkedHashMap<>();

    public ForMap entry(String key, ValAssert value) {
      expectedElements.put(key, value);
      return this;
    }

    public ForMap entry(String key, Object value) {
      expectedElements.put(key, ValAssert.of(value));
      return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(String parentPath, Value<?> anyValue) {
      String pathPrefix = parentPath == null ? "" : parentPath + ".";
      assertThat(anyValue.getType())
          .describedAs(descriptionForPath(parentPath, "to be a map"))
          .isEqualTo(ValueType.KEY_VALUE_LIST);
      expectedElements.forEach(
          (key, valueAssert) -> {
            Optional<KeyValue> first =
                ((List<KeyValue>) anyValue.getValue())
                    .stream().filter(kv -> kv.getKey().equals(key)).findFirst();

            String path = pathPrefix + key;
            assertThat(first)
                .describedAs(descriptionForPath(path, "to exist / be non-null"))
                .isPresent();
            valueAssert.accept(path, first.get().getValue());
          });
    }
  }

  class ForArray implements ValAssert {

    private final List<ValAssert> expectedElements = new ArrayList<>();
    private boolean ignoreOrder = false;

    public ForArray entry(ValAssert value) {
      expectedElements.add(value);
      return this;
    }

    public ForArray ignoreOrder() {
      ignoreOrder = true;
      return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void accept(String parentPath, Value anyValue) {
      String pathPrefix = parentPath == null ? "" : parentPath;
      assertThat(anyValue.getType())
          .describedAs(descriptionForPath(parentPath, "to be an array"))
          .isEqualTo(ValueType.ARRAY);

      List<Value<?>> arrayVal = (List<Value<?>>) anyValue.getValue();
      assertThat(arrayVal.size())
          .describedAs(descriptionForPath(parentPath, "to have the exact array size"))
          .isEqualTo(expectedElements.size());
      if (ignoreOrder) {
        for (ValAssert assertion : expectedElements) {
          assertThat(arrayVal)
              .describedAs(descriptionForPath(parentPath, "to have any element satisfying"))
              .anySatisfy(val -> assertion.accept(pathPrefix + "[x]", val));
        }
      } else {
        for (int i = 0; i < expectedElements.size(); i++) {
          String path = pathPrefix + "[" + i + "]";
          expectedElements.get(i).accept(path, arrayVal.get(i));
        }
      }
    }
  }
}
