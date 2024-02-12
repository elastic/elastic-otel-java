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
package co.elastic.otel.common;


import com.blogspot.mydailyjava.weaklockfree.WeakConcurrentMap;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.FieldBackedSpanValueStorageProvider;
import javax.annotation.Nullable;

public interface SpanValueStorageProvider {

  Class<?> SDK_SPAN_CLASS = getSdkSpanClass();

  static SpanValueStorageProvider get() {
    return FieldBackedSpanValueStorageProvider.INSTANCE != null
        ? FieldBackedSpanValueStorageProvider.INSTANCE : new MapBacked();
  }


  @Nullable
  SpanValueStorage get(Span span, boolean initialize);


  class MapBacked implements SpanValueStorageProvider {

    private final WeakConcurrentMap<Span, SpanValueStorage> storageMap =
        WeakConcurrent.createMap();

    @Override
    public SpanValueStorage get(Span span, boolean initialize) {
      SpanValueStorage storage = storageMap.get(span);
      if (storage == null && initialize) {
        storage = new SpanValueStorage();
        storageMap.putIfAbsent(span, storage);
        storage = storageMap.get(span);
      }
      return storage;
    }
  }

  static Class<?> getSdkSpanClass() {
    try {
      return Class.forName("io.opentelemetry.sdk.trace.SdkSpan");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Expected class to exist", e);
    }
  }
}
