package co.elastic.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.LocalRootSpan;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ElasticBreakdownMetrics {

    // map span context to local root span context (one entry per in-flight span)
    private final ConcurrentHashMap<SpanContext, SpanContext> localRootSpans;

    // for local root spans, map span context to our own sidecar object
    private final ConcurrentHashMap<SpanContext, BreakdownMetricsData> localRootSpanData;

    private OpenTelemetry openTelemetry;
    private ElasticSpanExporter spanExporter;

    public ElasticBreakdownMetrics() {
        localRootSpanData = new ConcurrentHashMap<>();
        localRootSpans = new ConcurrentHashMap<>();
    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    public void onSpanStart(Context parentContext, ReadWriteSpan span) {
        // unfortunately we can't cast to SdkSpan as it's loaded in AgentClassloader and the extension is loaded
        // in the ExtensionClassloader, hence we can't use the package-protected SdkSpan#getStartEpochNanos method
        //
        // However, adding accessors to the start/and timestamps to the ReadableSpan interface could be something we
        // could attempt to contribute.
        long spanStart = span.toSpanData().getStartEpochNanos();

        SpanContext spanContext = span.getSpanContext();
        SpanContext localRootSpanContext;
        // We can use this because we have a read write span here
        // alternatively we could have resolved the parent span on the parent context to get parent span context.
        if (isRootSpanParent(span.getParentSpanContext())) {
            // the span is a local root span
            localRootSpanContext = spanContext;

            System.out.printf("starting a local root span%s%n", localRootSpanContext.getSpanId());
            localRootSpans.put(localRootSpanContext, localRootSpanContext);
            localRootSpanData.put(localRootSpanContext, new BreakdownMetricsData(spanStart));
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
                breakdownData.startChild(spanStart);
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
            // TODO: we still have to convert to span data to get the end timestamp
            long selfTime = breakdownData.endLocalRootSpan(span.toSpanData().getEndEpochNanos());
            if(spanExporter != null) {
                spanExporter.reportSelfTime(spanContext, selfTime);
            }
        } else {
            System.out.printf("end of child span %s, root = %s%n", spanContext.getSpanId(), localRootSpanContext.getSpanId());
            if (localRootSpanContext.isValid()) {
                BreakdownMetricsData breakdownData = localRootSpanData.get(localRootSpanContext);
                if (breakdownData != null) {
                    // TODO: we still have to convert to span data to get the end timestamp
                    breakdownData.endChild(span.toSpanData().getEndEpochNanos());
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

    public void registerSpanExporter(ElasticSpanExporter spanExporter) {
        this.spanExporter = spanExporter;
    }

    // TODO: 06/09/2023 make this data thread-safe for reliable accounting
    private static class BreakdownMetricsData {
        public static final Clock clock = Clock.getDefault();
        // transaction self time
        private final AtomicInteger activeChildren;

        private long startEpochNanos;

        // timestamp of the 1st child start
        private long childStartEpoch;

        // duration for which there was a child span execution
        private long childDuration;

        public BreakdownMetricsData(long startEpochNanos) {
            this.activeChildren = new AtomicInteger();
            this.startEpochNanos = startEpochNanos;
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
                childDuration += (endEpochNanos - childStartEpoch);
                childStartEpoch = -1L;
            }
            System.out.printf("end child span, count = %d%n", count);
        }

        /**
         * @param endEpochNanos span end timestamp
         * @return span self time
         */
        public long endLocalRootSpan(long endEpochNanos) {
            if (childStartEpoch > 0) {
                childDuration += (clock.now() - childStartEpoch);
            }
            return endEpochNanos - startEpochNanos - childDuration;
        }
    }


}
