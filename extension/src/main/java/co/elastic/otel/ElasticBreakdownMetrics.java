package co.elastic.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkSpanAccessor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ElasticBreakdownMetrics {

    // map span context to local root span context (one entry per in-flight span)
    private final ConcurrentHashMap<SpanContext, SpanContext> localRootSpans;

    // for local root spans, map span context to our own sidecar object
    private final ConcurrentHashMap<SpanContext, BreakdownMetricsData> localRootSpanData;

    public static final ElasticBreakdownMetrics INSTANCE = new ElasticBreakdownMetrics();
    private OpenTelemetry openTelemetry;

    public ElasticBreakdownMetrics() {
        localRootSpanData = new ConcurrentHashMap<>();
        localRootSpans = new ConcurrentHashMap<>();
    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    public void onSpanStart(Context parentContext, ReadWriteSpan span) {

        SpanContext spanContext = span.getSpanContext();
        SpanContext localRootSpanContext;
        // We can use this because we have a read write span here
        // alternatively we could have resolved the parent span on the parent context to get parent span context.
        if (isRootSpanParent(span.getParentSpanContext())) {
            // the span is a local root span
            localRootSpanContext = spanContext;

            System.out.printf("starting a local root span%s%n", localRootSpanContext.getSpanId());
            localRootSpans.put(localRootSpanContext, localRootSpanContext);
            localRootSpanData.put(localRootSpanContext, new BreakdownMetricsData());
        } else {
            // the current span is a child (or grand-child) of the local root span
            // we can attempt to capture the "local root span" stored in context (if there is any)
            // and fallback to Elastic storage for lookup.
            localRootSpanContext = LocalRootSpan.fromContext(parentContext).getSpanContext();
            if (!localRootSpanContext.isValid()) {
                localRootSpanContext = lookupLocalRootSpan(span.getParentSpanContext());
            }
            if (localRootSpanContext.isValid()) {
                localRootSpans.put(spanContext, localRootSpanContext);
            }
            System.out.printf("start of child span %s, root = %s%n", spanContext.getSpanId(), localRootSpanContext.getSpanId());
            if (localRootSpanContext.isValid()) {
                BreakdownMetricsData breakdownData = localRootSpanData.get(localRootSpanContext);
                breakdownData.startChild(SdkSpanAccessor.getStartEpochNanos(span));
            }
        }

    }

    public void onSpanEnd(ReadableSpan span) {
        SpanContext spanContext = span.getSpanContext();

        // retrieve local root span from storage
        SpanContext localRootSpanContext = lookupLocalRootSpan(spanContext);

        if (isRootSpanParent(span.getParentSpanContext())) {
            System.out.printf("end of local root span %s%n", spanContext.getSpanId());
            localRootSpans.remove(spanContext);
            BreakdownMetricsData breakdownData = localRootSpanData.remove(spanContext);
            if (breakdownData == null) {
                throw new IllegalStateException("local root data has already been removed");
            }
        } else {
            System.out.printf("end of child span %s, root = %s%n", spanContext.getSpanId(), localRootSpanContext.getSpanId());
            if (localRootSpanContext.isValid()) {
                BreakdownMetricsData breakdownData = localRootSpanData.get(localRootSpanContext);
                if (breakdownData != null) {
                    // because span is ended, the 'span latency' returns the span end timestamp
                    breakdownData.endChild(span.getLatencyNanos());
                }
            }
            localRootSpans.remove(spanContext);
        }
    }


    private SpanContext lookupLocalRootSpan(SpanContext spanContext) {
        SpanContext localRoot = localRootSpans.get(spanContext);
        return localRoot != null ? localRoot : SpanContext.getInvalid();
    }

    private static boolean isRootSpanParent(SpanContext parentSpanContext) {
        return !parentSpanContext.isValid() || parentSpanContext.isRemote();
    }

    // TODO: 06/09/2023 make this data thread-safe for reliable accounting
    private static class BreakdownMetricsData {
        // transaction self time
        private final AtomicInteger activeChildren;

        private long rootSpanStartEpoch;

        // timestamp of the 1st child start
        private long childStartEpoch;

        // duration for which there was a child span execution
        private long childDuration;

        public BreakdownMetricsData() {
            this.activeChildren = new AtomicInteger();
        }

        public void startChild(long startEpochNanos) {
            int count = activeChildren.incrementAndGet();
            if (count == 1) {
                childStartEpoch = startEpochNanos;
            }
            System.out.printf("start child span, count = %d%n", count);
        }

        public void endChild(long endEpochNanos) {
            int count = activeChildren.decrementAndGet();
            if (count == 0) {
                childDuration += endEpochNanos - childStartEpoch;
            }
            System.out.printf("end child span, count = %d%n", count);
        }

        public long getRootSpanSelfTime() {
            return -1L;
        }
    }


}
