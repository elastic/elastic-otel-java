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
package co.elastic.otel;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public class SpanByIdSet {

  /**
   * The value of an entry is always the same as the key in this map (=it is a set). We just use a
   * map to allow a lookup of the original {@link WeakSpanWithId} via a {@link LookupKey}. While the
   * key type of this map is {@link TraceIdKeyed}, it only stores {@link WeakSpanWithId}s.
   */
  private final Map<TraceIdKeyed, WeakSpanWithId> spansById = new ConcurrentHashMap<>();

  private final ReferenceQueue<ReadableSpan> collectedSpansQueue = new ReferenceQueue<>();

  public void add(ReadableSpan span) {
    WeakSpanWithId wrapper = new WeakSpanWithId(span, collectedSpansQueue);
    spansById.putIfAbsent(wrapper, wrapper);
  }

  public void remove(ReadableSpan span) {
    SpanContext ctx = span.getSpanContext();
    LookupKey key = new LookupKey(ctx.getTraceId(), ctx.getSpanId());
    spansById.remove(key);
  }

  public synchronized void expungeStaleEntries() {
    Reference<? extends ReadableSpan> ref;
    while ((ref = collectedSpansQueue.poll()) != null) {
      WeakSpanWithId elem = ((WeakSpanWithId) ref);
      spansById.remove(elem);
    }
  }

  @Nullable
  public ReadableSpan get(String traceId, String spanId) {
    LookupKey key = new LookupKey(traceId, spanId);
    WeakSpanWithId result = (WeakSpanWithId) spansById.get(key);
    return result != null ? result.get() : null;
  }

  // For testing only
  int size() {
    return spansById.size();
  }

  private interface TraceIdKeyed {

    String getTraceId();

    String getSpanId();

    static boolean equals(TraceIdKeyed self, Object o) {
      if (!(o instanceof TraceIdKeyed)) {
        return false;
      }

      TraceIdKeyed that = (TraceIdKeyed) o;

      if (!self.getTraceId().equals(that.getTraceId())) {
        return false;
      }
      return self.getSpanId().equals(that.getSpanId());
    }

    static int hashCode(TraceIdKeyed self) {
      int result = self.getTraceId().hashCode();
      result = 31 * result + self.getSpanId().hashCode();
      return result;
    }
  }

  private static class WeakSpanWithId extends WeakReference<ReadableSpan> implements TraceIdKeyed {
    private final String traceId;
    private final String spanId;

    WeakSpanWithId(ReadableSpan span, ReferenceQueue<ReadableSpan> refQueue) {
      super(span, refQueue);
      SpanContext ctx = span.getSpanContext();
      traceId = ctx.getTraceId();
      spanId = ctx.getSpanId();
      assertLowerCase(traceId);
      assertLowerCase(spanId);
    }

    @Override
    public boolean equals(Object o) {
      return TraceIdKeyed.equals(this, o);
    }

    @Override
    public int hashCode() {
      return TraceIdKeyed.hashCode(this);
    }

    @Override
    public String getTraceId() {
      return traceId;
    }

    @Override
    public String getSpanId() {
      return spanId;
    }
  }

  private static class LookupKey implements TraceIdKeyed {
    private final String traceId;
    private final String spanId;

    public LookupKey(String traceId, String spanId) {
      this.traceId = traceId;
      this.spanId = spanId;
      assertLowerCase(traceId);
      assertLowerCase(spanId);
    }

    @Override
    public boolean equals(Object o) {
      return TraceIdKeyed.equals(this, o);
    }

    @Override
    public int hashCode() {
      return TraceIdKeyed.hashCode(this);
    }

    @Override
    public String getTraceId() {
      return traceId;
    }

    @Override
    public String getSpanId() {
      return spanId;
    }
  }

  private static void assertLowerCase(String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (Character.isAlphabetic(c) && !Character.isLowerCase(c)) {
        throw new IllegalArgumentException("Expected string to be lower case: " + str);
      }
    }
  }
}
