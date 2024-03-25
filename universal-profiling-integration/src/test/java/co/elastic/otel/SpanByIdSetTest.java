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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import co.elastic.otel.common.util.HexUtils;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpanByIdSetTest {

  private final Random rnd = new Random(12345);

  private SpanByIdSet set;

  @BeforeEach
  public void init() {
    set = new SpanByIdSet();
  }

  @Test
  public void checkLookup() {
    ReadableSpan ab = mockSpan("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb");
    ReadableSpan ac = mockSpan("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "cccccccccccccccc");
    ReadableSpan db = mockSpan("dddddddddddddddddddddddddddddddd", "bbbbbbbbbbbbbbbb");
    ReadableSpan dc = mockSpan("dddddddddddddddddddddddddddddddd", "cccccccccccccccc");

    set.add(ab);
    set.add(ac);
    set.add(db);
    set.add(dc);

    assertThat(set.get("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb")).isSameAs(ab);
    assertThat(set.get("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "cccccccccccccccc")).isSameAs(ac);
    assertThat(set.get("dddddddddddddddddddddddddddddddd", "bbbbbbbbbbbbbbbb")).isSameAs(db);
    assertThat(set.get("dddddddddddddddddddddddddddddddd", "cccccccccccccccc")).isSameAs(dc);
  }

  @Test
  public void duplicateAddAndRemove() {
    ReadableSpan span = mockSpan();
    SpanContext ctx = span.getSpanContext();

    set.add(span);
    assertThat(set.get(ctx.getTraceId(), ctx.getSpanId())).isSameAs(span);

    set.add(span);
    assertThat(set.get(ctx.getTraceId(), ctx.getSpanId())).isSameAs(span);

    set.remove(span);
    assertThat(set.get(ctx.getTraceId(), ctx.getSpanId())).isNull();
    set.remove(span);
    assertThat(set.get(ctx.getTraceId(), ctx.getSpanId())).isNull();
  }

  @Test
  public void testStaleEntriesExpunged() {
    ReadableSpan span1 = mockSpan();
    ReadableSpan span2 = mockSpan();
    SpanContext sp1Ctx = span1.getSpanContext();
    SpanContext sp2Ctx = span2.getSpanContext();

    WeakReference<ReadableSpan> weakSp1 = new WeakReference<>(span1);

    set.add(span1);
    set.add(span2);

    assertThat(set.size()).isEqualTo(2);
    assertThat(set.get(sp1Ctx.getTraceId(), sp1Ctx.getSpanId())).isSameAs(span1);
    assertThat(set.get(sp2Ctx.getTraceId(), sp2Ctx.getSpanId())).isSameAs(span2);

    span1 = null;
    waitToBeGCed(weakSp1);

    assertThat(set.size()).isEqualTo(2);
    assertThat(set.get(sp1Ctx.getTraceId(), sp1Ctx.getSpanId())).isNull();
    assertThat(set.get(sp2Ctx.getTraceId(), sp2Ctx.getSpanId())).isSameAs(span2);

    // Test failureshave shown that the weak references are not necessarily
    // added to the queue immediately, so we retry for a limited amount of time
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
      set.expungeStaleEntries();

      assertThat(set.size()).isEqualTo(1);
      assertThat(set.get(sp1Ctx.getTraceId(), sp1Ctx.getSpanId())).isNull();
      assertThat(set.get(sp2Ctx.getTraceId(), sp2Ctx.getSpanId())).isSameAs(span2);
    });
  }

  private static void waitToBeGCed(WeakReference<ReadableSpan> weakSp1) {
    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(1))
        .until(
            () -> {
              System.gc();
              return weakSp1.get() == null;
            });
  }

  private ReadableSpan mockSpan() {
    StringBuilder traceId = new StringBuilder();
    HexUtils.appendLongAsHex(rnd.nextLong(), traceId);
    HexUtils.appendLongAsHex(rnd.nextLong(), traceId);
    StringBuilder spanId = new StringBuilder();
    HexUtils.appendLongAsHex(rnd.nextLong(), spanId);
    return mockSpan(traceId.toString(), spanId.toString());
  }

  private ReadableSpan mockSpan(String traceId, String spanId) {
    SpanContext ctx =
        SpanContext.create(traceId, spanId, TraceFlags.getDefault(), TraceState.getDefault());
    ReadableSpan result = mock(ReadableSpan.class);
    doReturn(ctx).when(result).getSpanContext();
    return result;
  }
}
