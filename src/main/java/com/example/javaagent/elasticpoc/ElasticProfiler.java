package com.example.javaagent.elasticpoc;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ElasticProfiler {

    private final ConcurrentHashMap<SpanContext, List<Long>> spanSamples;
    private final ConcurrentHashMap<SpanContext, List<StackTraceElement[]>> spanStackTraces;
    private final ConcurrentHashMap<Long, SpanContext> currentSpans;

    private final ScheduledExecutorService samplerExecutor;

    private final Clock clock = Clock.getDefault();
    private SpanExporter exporter;


    public ElasticProfiler() {
        spanSamples = new ConcurrentHashMap<>();
        currentSpans = new ConcurrentHashMap<>();
        spanStackTraces = new ConcurrentHashMap<>();

        samplerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        samplerExecutor.scheduleAtFixedRate(() -> {
            for (Map.Entry<Long, SpanContext> entry : currentSpans.entrySet()) {
                SpanContext spanContext = entry.getValue();
                System.out.printf("sampling span %s on thread %d%n", spanContext.getSpanId(), entry.getKey());

                ThreadInfo threadInfo = threadMXBean.getThreadInfo(entry.getKey(), 50);

                spanSamples.compute(spanContext, (s, sampleTimestamps) -> {
                    if (sampleTimestamps == null) {
                        sampleTimestamps = new ArrayList<>();
                    }
                    sampleTimestamps.add(clock.now());
                    return sampleTimestamps;
                });

                // wip: trying to do the same with actual stack traces
                spanStackTraces.compute(spanContext, (s, stackTraces) -> {
                    if (stackTraces == null) {
                        stackTraces = new ArrayList<>();
                    }
                    stackTraces.add(threadInfo.getStackTrace());
                    return stackTraces;
                });
            }
            // increment counters
        }, 0, 10, TimeUnit.MILLISECONDS);
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

    public void onSpanStart(Context parentContext, ReadWriteSpan span) {
        Span parentSpan = Span.fromContext(parentContext);
        SpanContext parentSpanContext = parentSpan.getSpanContext();
        if (parentSpanContext.isValid()) {
            // starting a span over an existing one
            // we can flush the samples that might have been captured on the parent span
            spanifySamples(parentSpanContext);
        }

    }

    public void onSpanEnd(ReadableSpan span) {
        spanifySamples(span.getSpanContext());
    }

    public void shutdown() {
        spanSamples.clear();
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

    private void spanifySamples(SpanContext spanContext) {
        // wip: for now just discard stack traces
        spanStackTraces.remove(spanContext);

        List<Long> samples = spanSamples.remove(spanContext);
        if (samples == null || samples.size() < 2) {
            return;
        }

        long start = samples.get(0);
        long end = samples.get(samples.size() - 1);
        String spanId = IdGenerator.random().generateSpanId();

        exporter.export(Collections.singletonList(new SpanData() {
            @Override
            public String getName() {
                return String.format("inferred (%s)", samples.size());
            }

            @Override
            public SpanKind getKind() {
                return SpanKind.INTERNAL;
            }

            @Override
            public SpanContext getSpanContext() {
                return SpanContext.create(spanContext.getTraceId(), spanId, spanContext.getTraceFlags(), spanContext.getTraceState());
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
                return Attributes.empty();
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
                return Resource.empty();
            }
        }));
    }


}
