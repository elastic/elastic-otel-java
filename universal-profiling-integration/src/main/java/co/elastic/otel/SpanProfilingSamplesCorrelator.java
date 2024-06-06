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

import co.elastic.otel.common.ElasticAttributes;
import co.elastic.otel.common.LocalRootSpan;
import co.elastic.otel.common.SpanValue;
import co.elastic.otel.disruptor.FreezableList;
import co.elastic.otel.disruptor.MoveableEvent;
import co.elastic.otel.disruptor.PeekingPoller;
import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.stacktrace.internal.MutableSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.HdrHistogram.WriterReaderPhaser;

public class SpanProfilingSamplesCorrelator {

  private static final Logger logger =
      Logger.getLogger(SpanProfilingSamplesCorrelator.class.getName());

  private static final SpanValue<FreezableList<String>> profilerStackTraceIds =
      SpanValue.createSparse();

  private final SpanByIdSet spansById = new SpanByIdSet();

  private final Consumer<ReadableSpan> sendSpan;

  private final LongSupplier nanoClock;

  // Visible for testing
  final RingBuffer<DelayedSpan> delayedSpans;
  private final PeekingPoller<DelayedSpan> delayedSpansPoller;

  private volatile long spanBufferDurationNanos;
  private volatile boolean shuttingDown = false;

  private final WriterReaderPhaser shutdownPhaser = new WriterReaderPhaser();

  public SpanProfilingSamplesCorrelator(
      int bufferCapacity,
      LongSupplier nanoClock,
      long initialSpanDelayNanos,
      Consumer<ReadableSpan> sendSpan) {
    this.nanoClock = nanoClock;
    this.spanBufferDurationNanos = initialSpanDelayNanos;
    this.sendSpan = sendSpan;

    bufferCapacity = nextPowerOf2(bufferCapacity);
    // We use a wait strategy which doesn't involve signaling via condition variables
    // because we never block anyway (we use polling)
    delayedSpans =
        RingBuffer.createMultiProducer(
            DelayedSpan::new, bufferCapacity, new YieldingWaitStrategy());
    EventPoller<DelayedSpan> nonPeekingPoller = delayedSpans.newPoller();
    delayedSpans.addGatingSequences(nonPeekingPoller.getSequence());

    delayedSpansPoller = new PeekingPoller<>(nonPeekingPoller, DelayedSpan::new);
  }

  public void setSpanBufferDurationNanos(long nanos) {
    if (nanos < 0) {
      throw new IllegalArgumentException("nanos must be positive but was " + nanos);
    }
    spanBufferDurationNanos = nanos;
  }

  public void onSpanStart(ReadableSpan span, Context parentCtx) {
    boolean sampled = span.getSpanContext().getTraceFlags().isSampled();
    boolean isLocalRoot = LocalRootSpan.getFor(span) == span;
    if (sampled && isLocalRoot) {
      spansById.add(span);
    }
  }

  public void sendOrBufferSpan(ReadableSpan span) {
    boolean sampled = span.getSpanContext().getTraceFlags().isSampled();
    if (!sampled || LocalRootSpan.getFor(span) != span) {
      sendSpan.accept(span);
      return;
    }

    long criticalPhaseVal = shutdownPhaser.writerCriticalSectionEnter();
    try {
      if (spanBufferDurationNanos == 0 || shuttingDown) {
        correlateAndSendSpan(span);
        return;
      }

      boolean couldPublish =
          delayedSpans.tryPublishEvent(
              (event, idx, sp, timestamp) -> {
                event.span = sp;
                event.endNanoTimestamp = timestamp;
              },
              span,
              nanoClock.getAsLong());

      if (!couldPublish) {
        logger.log(
            Level.WARNING,
            "The following span could not be delayed for correlation due to a full buffer, it will be sent immediately, {0}",
            span);
        correlateAndSendSpan(span);
      }
    } finally {
      shutdownPhaser.writerCriticalSectionExit(criticalPhaseVal);
    }
  }

  public void correlate(
      String traceId, String localRootSpanId, CharSequence stackTraceId, int count) {
    ReadableSpan span = spansById.get(traceId, localRootSpanId);
    if (span != null) {
      FreezableList<String> list = profilerStackTraceIds.computeIfNull(span, FreezableList::new);
      String stId = stackTraceId.toString();
      for (int i = 0; i < count; i++) {
        list.addIfNotFrozen(stId);
      }
    }
  }

  public synchronized void flushPendingBufferedSpans() {
    try {
      delayedSpansPoller.poll(
          bufferedSpan -> {
            long elapsed = nanoClock.getAsLong() - bufferedSpan.endNanoTimestamp;
            if (elapsed >= spanBufferDurationNanos || shuttingDown) {
              correlateAndSendSpan(bufferedSpan.span);
              bufferedSpan.clear();
              return true;
            }
            return false; // span is not yet ready to be sent
          });
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    spansById.expungeStaleEntries();
  }

  public synchronized void shutdownAndFlushAll() {
    shutdownPhaser.readerLock();
    try {
      shuttingDown = true; // This will cause new ended spans to not be buffered anymore

      // avoid race condition: we wait until we are
      // sure that no more spans will be added to the ringbuffer
      shutdownPhaser.flipPhase();
    } finally {
      shutdownPhaser.readerUnlock();
    }
    // This will flush all pending spans because shuttingDown=true
    flushPendingBufferedSpans();
  }

  private void correlateAndSendSpan(ReadableSpan span) {
    spansById.remove(span);
    FreezableList<String> list = profilerStackTraceIds.get(span);
    if (list != null) {
      MutableSpan mutableSpan = MutableSpan.makeMutable(span);
      mutableSpan.setAttribute(ElasticAttributes.PROFILER_STACK_TRACE_IDS, list.freezeAndGet());
      sendSpan.accept(mutableSpan);
    } else {
      sendSpan.accept(span);
    }
  }

  private static class DelayedSpan implements MoveableEvent<DelayedSpan> {

    ReadableSpan span;
    long endNanoTimestamp;

    @Override
    public void moveInto(DelayedSpan other) {
      other.span = span;
      other.endNanoTimestamp = endNanoTimestamp;
      clear();
    }

    @Override
    public void clear() {
      span = null;
      endNanoTimestamp = -1;
    }
  }

  private static int nextPowerOf2(int val) {
    int result = 2;
    while (result < val) {
      result *= 2;
    }
    return result;
  }
}
