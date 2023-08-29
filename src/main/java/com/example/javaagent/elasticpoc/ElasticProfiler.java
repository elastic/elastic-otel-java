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
    private final ConcurrentHashMap<Long, SpanContext> currentSpans;

    private final ScheduledExecutorService samplerExecutor;

    private final Clock clock = Clock.getDefault();
    private SpanExporter exporter;


    public ElasticProfiler() {
        spanSamples = new ConcurrentHashMap<>();
        currentSpans = new ConcurrentHashMap<>();

        samplerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });

        samplerExecutor.scheduleAtFixedRate(() -> {
            for (Map.Entry<Long, SpanContext> entry : currentSpans.entrySet()) {
                SpanContext spanContext = entry.getValue();
                spanSamples.compute(spanContext, (s, sampleTimestamps) -> {
                    if (sampleTimestamps == null) {
                        sampleTimestamps = new ArrayList<>();
                    }
                    sampleTimestamps.add(clock.now());
                    return sampleTimestamps;
                });
            }
            // increment counters
        }, 0, 10, TimeUnit.MILLISECONDS);
    }

    public void registerExporter(SpanExporter exporter) {
        this.exporter = exporter;
    }

    public void onSpanContextAttach(Span span) {
        // not used for now
    }

    public void onSpanContextClose(Span span) {
        // not used for now
        //
        // the issue here with context activation wrapping is that we don't capture the span end properly, thus it's
        // harder to accurately measure the sampled spans timings
    }

    public void onSpanStart(Context parentContext, ReadWriteSpan span) {
        Span parentSpan = Span.fromContext(parentContext);
        SpanContext parentSpanContext = span.getSpanContext();
        if (parentSpanContext.isValid()) {
            // starting a span over an existing one
            // we can flush the samples that might have been captured on the parent span
            spanifySamples(parentSpanContext);
        }
        long id = Thread.currentThread().getId();
        currentSpans.put(id, span.getSpanContext());
    }

    public void onSpanEnd(ReadableSpan span) {
        SpanContext parentSpanContext = span.getParentSpanContext();
        long id = Thread.currentThread().getId();
        if (parentSpanContext.isValid()) {
            currentSpans.put(id, parentSpanContext);
        } else {
            currentSpans.remove(id);
        }

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
