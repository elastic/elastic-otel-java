package co.elastic.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ElasticBreakdownMetrics {

    // map span context to local root span context (one entry per in-flight span)
    private final ConcurrentHashMap<SpanContext, SpanContext> localRootSpans;

    // for local root spans, map span context to our own sidecar object
    private final ConcurrentHashMap<SpanContext, BreakdownMetricsData> localRootSpanData;

    public static final ElasticBreakdownMetrics INSTANCE = new ElasticBreakdownMetrics();

    public ElasticBreakdownMetrics() {
        localRootSpanData = new ConcurrentHashMap<>();
        localRootSpans = new ConcurrentHashMap<>();
    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {

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
            if(breakdownData == null) {
                throw new IllegalStateException("local root data has already beeen removed");
            }
        } else {
            System.out.printf("end of child span %s, root = %s%n", spanContext.getSpanId(), localRootSpanContext.getSpanId());
            if (localRootSpanContext.isValid()) {
                BreakdownMetricsData breakdownData = localRootSpanData.get(localRootSpanContext);
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

    private static class BreakdownMetricsData {
        // transaction self time
        private final AtomicInteger activeChildren;
        private long childStartEpoch;
        private long childDuration;

        public BreakdownMetricsData() {
            this.activeChildren = new AtomicInteger();
        }

        public void startChild(long startEpochNanos) {
            if (activeChildren.incrementAndGet() == 1) {
                childStartEpoch = startEpochNanos;
            }
        }

        public void endChild(SpanData span) {
            if (activeChildren.decrementAndGet() == 0) {
                childDuration += span.getEndEpochNanos() - childStartEpoch;
            }

        }
    }


}
