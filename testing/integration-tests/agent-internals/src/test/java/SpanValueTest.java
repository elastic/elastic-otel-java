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
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.junit.jupiter.api.Test;

public class SpanValueTest {
  @Test
  @SuppressWarnings("unchecked")
  public void ensureStoredAsFields() throws Exception {
    // This test heavily depends on agent internals and this is intentional
    // It breaks into the agent to ensure that the more performant field-backed storage is used
    // for co.elastic.otel.common.SpanValues

    Tracer tracer = GlobalOpenTelemetry.get().getTracer("my-tracer");
    Span bridgeSpan = tracer.spanBuilder("s1").startSpan();

    // Move from the bridged span to the agent internal Span instance (of the Agent's SDK).
    Object agentSpan = readFieldValue(bridgeSpan, "agentSpan");
    assertThat(agentSpan.getClass().getSimpleName()).isEqualTo("SdkSpan");

    // From that Classloader we should be able to find and create SpanValues
    Class<?> spanValueClass =
        Class.forName(
            "co.elastic.otel.common.SpanValue", true, agentSpan.getClass().getClassLoader());
    Class<?> readableSpanInterface =
        Class.forName(
            "io.opentelemetry.sdk.trace.ReadableSpan", true, agentSpan.getClass().getClassLoader());
    Object denseSpanValue = spanValueClass.getMethod("createDense").invoke(null);

    // create a new span which has a space for our newly created SpanValue allocated

    Span bridgeSpan2 = tracer.spanBuilder("s2").startSpan();
    Object agentSpan2 = readFieldValue(bridgeSpan2, "agentSpan");

    spanValueClass
        .getMethod("set", readableSpanInterface, Object.class)
        .invoke(denseSpanValue, agentSpan2, "foo");
    // Setting the value should initialize the backing AtomicReferenceArray on the span
    AtomicReferenceArray<Object> storage =
        (AtomicReferenceArray<Object>) readFieldValue(agentSpan2, "$elasticSpanValues");

    int storageIndex = -1;
    for (int i = 0; i < storage.length(); i++) {
      if ("foo".equals(storage.get(i))) {
        storageIndex = i;
      }
    }
    assertThat(storageIndex).isNotEqualTo(-1);

    // modify the value and make sure the span value reads it
    // this excludes the possibility of there being a map based storage used in addition
    storage.set(storageIndex, "bar");

    Object value =
        spanValueClass.getMethod("get", readableSpanInterface).invoke(denseSpanValue, agentSpan2);

    assertThat(value).isEqualTo("bar");
  }

  private static Object readFieldValue(Object instance, String field) throws Exception {
    Field fieldRef = instance.getClass().getDeclaredField(field);
    fieldRef.setAccessible(true);
    return fieldRef.get(instance);
  }
}
