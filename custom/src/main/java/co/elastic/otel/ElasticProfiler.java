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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ElasticProfiler {

  public static final int MAX_STACK_DEPTH = 50;
  private final ConcurrentHashMap<SpanContext, Samples> samplesMap;
  private final ConcurrentHashMap<Long, SpanContext> currentSpans;

  private final ScheduledExecutorService samplerExecutor;

  private final Clock clock = Clock.getDefault();
  private SpanExporter exporter;

  private LongCounter samples;
  private LongCounter inferredSpansCounter;

  public void registerOpenTelemetry(OpenTelemetry openTelemetry) {
    Meter meter = openTelemetry.getMeter("elastic.profiler");
    samples = meter.counterBuilder("inferred_spans_samples").build();
    inferredSpansCounter = meter.counterBuilder("inferred_spans").build();
  }

  private static class Sample {
    final long timestamp;
    final long threadId;
    final long sequenceId;
    final StackTraceElement[] stackTrace;

    Sample(long threadId, long timestamp, long sequenceId, StackTraceElement[] stackTrace) {
      this.threadId = threadId;
      this.timestamp = timestamp;
      this.sequenceId = sequenceId;
      this.stackTrace = stackTrace;
    }
  }

  private static class Samples {
    private SpanContext spanContext;
    private List<Sample> samples;

    Samples(SpanContext spanContext) {
      this.spanContext = spanContext;
      this.samples = new ArrayList<>();
    }

    void add(Sample sample) {
      samples.add(sample);
    }

    int size() {
      return samples.size();
    }
  }

  public ElasticProfiler() {
    currentSpans = new ConcurrentHashMap<>();
    samplesMap = new ConcurrentHashMap<>();

    samplerExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread thread = new Thread(r);
              thread.setDaemon(true);
              return thread;
            });

    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    AtomicLong sequenceId = new AtomicLong();

    samplerExecutor.scheduleAtFixedRate(
        () -> {
          long id = sequenceId.getAndIncrement();
          for (Map.Entry<Long, SpanContext> entry : currentSpans.entrySet()) {
            SpanContext spanContext = entry.getValue();
            Long threadId = entry.getKey();

            ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId, MAX_STACK_DEPTH);

            samplesMap.compute(
                spanContext,
                (s, samples) -> {
                  if (samples == null) {
                    samples = new Samples(spanContext);
                  }
                  samples.add(new Sample(threadId, clock.now(), id, threadInfo.getStackTrace()));
                  this.samples.add(1);
                  return samples;
                });
          }
          // increment counters
        },
        0,
        10,
        TimeUnit.MILLISECONDS);
  }

  public void registerExporter(SpanExporter exporter) {
    this.exporter = exporter;
  }

  public SpanContext onSpanContextAttach(SpanContext spanContext) {
    // store the active span on attach
    long id = Thread.currentThread().getId();
    SpanContext previous = currentSpans.put(id, spanContext);
    if (previous == null) {
      previous = SpanContext.getInvalid();
    }
    return previous;
  }

  public void onSpanContextClose(SpanContext previousSpanContext) {
    long id = Thread.currentThread().getId();
    if (previousSpanContext.isValid()) {
      // restore previous span context
      currentSpans.put(id, previousSpanContext);
    } else {
      // no previous span context to restore
      currentSpans.remove(id);
    }
  }

  public void onSpanStart(Context parentContext, ReadWriteSpan span) {}

  public void onSpanEnd(ReadableSpan span) {
    spanifySamples(span);
  }

  public void shutdown() {
    samplesMap.clear();
    currentSpans.clear();
    samplerExecutor.shutdown();
    try {
      if (!samplerExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
        samplerExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      samplerExecutor.shutdownNow();
    }
  }

  void spanifySamples(Resource resource, Samples samples) {
    if (samples.size() < 2) {
      return;
    }

    int i = 0;
    while (i < samples.size()) {

      // find a contiguous set of samples
      int j = i + 1;
      long sequenceId = samples.samples.get(i).sequenceId + 1;
      StackTraceElement[] firstStackTrace = samples.samples.get(i).stackTrace;
      while (j < samples.size()
          // samples must be contiguous
          && samples.samples.get(j).sequenceId == sequenceId
          // and have the same stack trace
          && Arrays.deepEquals(firstStackTrace, samples.samples.get(j).stackTrace)) {
        sequenceId++;
        j++;
      }
      int size = j - i;

      if (size >= 2) {
        Sample firstSample = samples.samples.get(i);
        Sample lastSample = samples.samples.get(j - 1);
        String spanId = IdGenerator.random().generateSpanId();

        String methodName = topMethodName(firstSample.stackTrace);
        exporter.export(
            Collections.singletonList(
                new InferredSpanData(
                    samples.size(),
                    methodName,
                    spanId,
                    samples.spanContext,
                    resource,
                    firstSample.timestamp,
                    lastSample.timestamp)));

        inferredSpansCounter.add(1);
      }

      i += size;
    }
  }

  private static String topMethodName(StackTraceElement[] stackTrace) {
    return String.format("%s#%s", stackTrace[0].getClassName(), stackTrace[0].getMethodName());
  }

  private void spanifySamples(ReadableSpan span) {
    SpanContext spanContext = span.getSpanContext();

    Samples samples = samplesMap.remove(spanContext);
    if (samples == null) {
      return;
    }

    spanifySamples(span.toSpanData().getResource(), samples);
  }

  private static class InferredSpanData implements SpanData {

    private static final AttributeKey<Long> SAMPLES_KEY = AttributeKey.longKey("profiling.samples");
    private final String name;
    private final String spanId;

    private final SpanContext spanContext;
    private final long start;
    private final long end;
    private final Resource resource;

    private final int samples;

    public InferredSpanData(
        int samples,
        String methodName,
        String spanId,
        SpanContext spanContext,
        Resource resource,
        long start,
        long end) {
      this.samples = samples;
      this.name = String.format("inferred (%s) - %s", samples, methodName);
      this.spanId = spanId;
      this.spanContext = spanContext;
      this.resource = resource;
      this.start = start;
      this.end = end;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public SpanKind getKind() {
      return SpanKind.INTERNAL;
    }

    @Override
    public SpanContext getSpanContext() {
      return SpanContext.create(
          spanContext.getTraceId(),
          spanId,
          spanContext.getTraceFlags(),
          spanContext.getTraceState());
    }

    @Override
    public SpanContext getParentSpanContext() {
      return spanContext;
    }

    @Override
    public StatusData getStatus() {
      return StatusData.ok();
    }

    @Override
    public long getStartEpochNanos() {
      return start;
    }

    @Override
    public Attributes getAttributes() {
      return Attributes.of(SAMPLES_KEY, (long) samples);
    }

    @Override
    public List<EventData> getEvents() {
      return Collections.emptyList();
    }

    @Override
    public List<LinkData> getLinks() {
      return Collections.emptyList();
    }

    @Override
    public long getEndEpochNanos() {
      return end;
    }

    @Override
    public boolean hasEnded() {
      return true;
    }

    @Override
    public int getTotalRecordedEvents() {
      return 0;
    }

    @Override
    public int getTotalRecordedLinks() {
      return 0;
    }

    @Override
    public int getTotalAttributeCount() {
      return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
      return InstrumentationLibraryInfo.empty();
    }

    @Override
    public Resource getResource() {
      return resource;
    }
  }
}
