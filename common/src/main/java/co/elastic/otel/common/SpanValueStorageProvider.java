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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public interface SpanValueStorageProvider {

  Logger logger = Logger.getLogger(SpanValueStorageProvider.class.getName());

  static SpanValueStorageProvider get() {
    try {
      Class<?> sdkSpan = Class.forName("io.opentelemetry.sdk.trace.SdkSpan");
      if (sdkSpan.getClassLoader() != SpanValueStorage.class.getClassLoader()) {
        // If we are running in a different classloader, this means we aren't running in our distro
        logger.log(
            Level.FINE,
            "Using map-backed storage for SpanValues because SdkSpan lives in a different classloader and therefore is inaccessible");
        return MapBacked.getInstance();
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Expected SdkSpan class to exist", e);
    }
    return FieldBackedSpanValueStorageProvider.INSTANCE != null
        ? FieldBackedSpanValueStorageProvider.INSTANCE
        : MapBacked.getInstance();
  }

  @Nullable
  SpanValueStorage get(Span span, boolean initialize);

  class MapBacked implements SpanValueStorageProvider {

    private static MapBacked INSTANCE;

    public static synchronized MapBacked getInstance() {
      // Lazy initialization to avoid unnecessary creation of the backing map
      if (INSTANCE == null) {
        INSTANCE = new MapBacked();
      }
      return INSTANCE;
    }

    private final WeakConcurrentMap<Span, SpanValueStorage> storageMap = WeakConcurrent.createMap();

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
}
