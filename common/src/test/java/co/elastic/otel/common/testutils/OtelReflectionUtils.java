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
package co.elastic.otel.common.testutils;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

public class OtelReflectionUtils {

  @SuppressWarnings("unchecked")
  public static List<SpanProcessor> getSpanProcessors(OpenTelemetry otel) {
    SdkTracerProvider tracer = unwrap(otel.getTracerProvider());
    SpanProcessor active =
        (SpanProcessor) readFieldChain(tracer, "sharedState", "activeSpanProcessor");
    return flattenCompositeProcessor(active, false);
  }

  public static List<SpanProcessor> flattenCompositeProcessors(SpanProcessor potentiallyComposite) {
    return flattenCompositeProcessor(potentiallyComposite, true);
  }

  public static List<SpanProcessor> flattenCompositeProcessor(
      SpanProcessor active, boolean recurse) {
    if (active.getClass().getName().contains("MultiSpanProcessor")) {
      List<SpanProcessor> childProcessors =
          (List<SpanProcessor>) readField(active, "spanProcessorsAll");
      if (recurse) {
        return childProcessors.stream()
            .flatMap(proc -> flattenCompositeProcessor(proc, true).stream())
            .collect(Collectors.toList());
      } else {
        return childProcessors;
      }
    } else {
      return Collections.singletonList(active);
    }
  }

  private static SdkTracerProvider unwrap(TracerProvider tracerProvider) {
    if (tracerProvider instanceof SdkTracerProvider) {
      return (SdkTracerProvider) tracerProvider;
    } else if (tracerProvider.getClass().getName().contains("ObfuscatedTracerProvider")) {
      TracerProvider delegate = (TracerProvider) readField(tracerProvider, "delegate");
      return unwrap(delegate);
    }
    throw new IllegalStateException("Unknown class: " + tracerProvider.getClass().getName());
  }

  private static Object readFieldChain(Object instance, String... fields) {
    Object current = instance;
    for (String fieldName : fields) {
      current = readField(current, fieldName);
    }
    return current;
  }

  private static Object readField(Object instance, String fieldName) {

    List<Field> fields =
        ReflectionSupport.findFields(
            instance.getClass(),
            field -> field.getName().equals(fieldName),
            HierarchyTraversalMode.BOTTOM_UP);

    if (fields.isEmpty()) {
      throw new IllegalArgumentException(
          instance.getClass().getName() + " does not have a field named '" + fieldName + "'");
    } else if (fields.size() > 2) {
      throw new IllegalArgumentException(
          instance.getClass().getName() + " has multiple fields named '" + fieldName + "'");
    }

    try {
      return ReflectionSupport.tryToReadFieldValue(fields.get(0), instance).get();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
