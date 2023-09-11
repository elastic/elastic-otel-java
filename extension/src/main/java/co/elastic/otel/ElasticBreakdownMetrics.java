package co.elastic.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ElasticBreakdownMetrics {

    private static final AttributeKey<String> LOCAL_ROOT_ATTRIBUTE = AttributeKey.stringKey("elastic.span.local_root");
    private static final AttributeKey<Boolean> IS_LOCAL_ROOT_ATTRIBUTE = AttributeKey.booleanKey("elastic.span.is_local_root");
    public static final AttributeKey<String> ELASTIC_SPAN_TYPE_ATTRIBUTE = AttributeKey.stringKey("elastic.span.type");
    public static final AttributeKey<String> ELASTIC_SPAN_SUBTYPE_ATTRIBUTE = AttributeKey.stringKey("elastic.span.subtype");

    private final ConcurrentHashMap<SpanContext, SpanContextData> elasticSpanData;

    private ElasticSpanExporter spanExporter;

    private Meter meter;

    // sidecar object we store for every span
    private class SpanContextData {

        private final SpanContext localRootSpan;

        private final ChildDuration childDuration;

        public SpanContextData(SpanContext localRoot, long start) {
            this.localRootSpan = localRoot;
            this.childDuration = new ChildDuration(start);
        }

    }

    public ElasticBreakdownMetrics() {
        elasticSpanData = new ConcurrentHashMap<>();
    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {
        meter = openTelemetry.getMeterProvider().meterBuilder("elastic.span_breakdown").build();
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
            elasticSpanData.put(spanContext, new SpanContextData(spanContext, spanStart));
        } else {
            // retrieve and store the local root span for later use
            localRootSpanContext = lookupLocalRootSpan(span.getParentSpanContext());
            if (localRootSpanContext.isValid()) {
                elasticSpanData.put(spanContext, new SpanContextData(localRootSpanContext, spanStart));
            }

            // update direct parent span child durations for self-time
            ChildDuration parentChildDuration = elasticSpanData.get(span.getParentSpanContext()).childDuration;
            if (parentChildDuration != null) {
                parentChildDuration.startChild(spanStart);
            }

            System.out.printf("start of child span %s, parent = %s, root = %s%n",
                    spanContext.getSpanId(),
                    span.getParentSpanContext().getSpanId(),
                    localRootSpanContext.getSpanId());
        }

        // we store extra attributes in span for later use, however we can't replace because we don't have access to the
        // attributes of the parent span, only its span context or the write-only Span
        span.setAttribute(IS_LOCAL_ROOT_ATTRIBUTE, localRootSpanContext == spanContext);
        span.setAttribute(LOCAL_ROOT_ATTRIBUTE, localRootSpanContext.getSpanId());

    }

    public void onSpanEnd(ReadableSpan span) {

        SpanContext spanContext = span.getSpanContext();

        // retrieve local root span from storage
        SpanContext localRootSpanContext = lookupLocalRootSpan(spanContext);

        SpanData spanData = span.toSpanData();

        // children duration for current span
        SpanContextData spanContextData = elasticSpanData.get(spanContext);
        Objects.requireNonNull(spanContextData, "missing elastic span data");

        // update children duration for direct parent
        SpanContextData parentSpanContextData = elasticSpanData.get(span.getParentSpanContext());
        if (parentSpanContextData != null) {
            // parent might be already terminated
            parentSpanContextData.childDuration.endChild(spanData.getEndEpochNanos());
        }

        long selfTime = spanContextData.childDuration.endSpan(spanData.getEndEpochNanos());

        // unfortunately here we get a read-only span that has already been ended, thus even a cast to ReadWriteSpan
        // does not allow us from adding extra span attributes
        if (spanExporter != null) {
            spanExporter.reportSelfTime(spanContext, selfTime);
        }

        if (isRootSpanParent(span.getParentSpanContext())) {
            System.out.printf("end of local root span %s%n", spanContext.getSpanId());

            // TODO: we don't have the local root span type and name here, we might need to store it for per transaction breakdown

            // report for current span
            Attributes metricAttributes = buildCounterAttributes(spanData.getAttributes());
            System.out.printf("%s %d %s%n", span.getSpanContext().getSpanId(), selfTime, metricAttributes);
            meter.counterBuilder("elastic.span_breakdown").build().add(selfTime, metricAttributes);
            // even when using a brand new counter instance still creates a sum
            // maybe reporting a complete histogram here would be a relevant option as the need is to get an overview of the time breakdown.

            // TODO report for every child span, which must have been stored (for now we only report it when they end, but I'm not sure if that's an issue)

        } else {
            System.out.printf("end of child span %s, root = %s%n", spanContext.getSpanId(), localRootSpanContext.getSpanId());

            Attributes metricAttributes = buildCounterAttributes(spanData.getAttributes());
            System.out.printf("%s %d %s%n", span.getSpanContext().getSpanId(), selfTime, metricAttributes);
            meter.counterBuilder("elastic.span_breakdown").build().add(selfTime, metricAttributes);
        }
        elasticSpanData.remove(spanContext);
    }

    private static Attributes buildCounterAttributes(Attributes attributes) {

        AtomicReference<String> type = new AtomicReference<>("app");
        AtomicReference<String> subType = new AtomicReference<>("internal");
        attributes.forEach((k, v) -> {
            String key = k.getKey();
            if (AttributeType.STRING.equals(k.getType())) {
                int index = key.indexOf(".system");
                if (index > 0) {
                    type.set(key.substring(0, index));
                    subType.set(v.toString());
                }
            }
        });

        return Attributes.of(
                ELASTIC_SPAN_TYPE_ATTRIBUTE, type.get(),
                ELASTIC_SPAN_SUBTYPE_ATTRIBUTE, subType.get()
        );
    }

    private SpanContext lookupLocalRootSpan(SpanContext spanContext) {
        SpanContextData spanContextData = elasticSpanData.get(spanContext);
        return spanContextData != null ? spanContextData.localRootSpan : SpanContext.getInvalid();
    }

    private static boolean isRootSpanParent(SpanContext parentSpanContext) {
        return !parentSpanContext.isValid() || parentSpanContext.isRemote();
    }

    public void registerSpanExporter(ElasticSpanExporter spanExporter) {
        this.spanExporter = spanExporter;
    }

    private static class ChildDuration {

        public static final Clock clock = Clock.getDefault();

        private final AtomicInteger activeChildren;

        private final long startEpochNanos;

        // timestamp of the 1st child start
        private long childStartEpoch;

        // duration for which there was at least child span executing
        private long childDuration;

        public ChildDuration(long startEpochNanos) {
            this.activeChildren = new AtomicInteger();
            this.startEpochNanos = startEpochNanos;
        }

        public void startChild(long startEpochNanos) {
            int count;
            synchronized (this) {
                count = activeChildren.incrementAndGet();
                if (count == 1) {
                    childStartEpoch = startEpochNanos;
                }
            }
            System.out.printf("start child span, count = %d%n", count);
        }

        public void endChild(long endEpochNanos) {

            int count;
            synchronized (this) {
                count = activeChildren.decrementAndGet();
                if (count == 0) {
                    childDuration += (endEpochNanos - childStartEpoch);
                    childStartEpoch = -1L;
                }
            }
            System.out.printf("end child span, count = %d%n", count);
        }

        /**
         * @param endEpochNanos span end timestamp
         * @return span self time
         */
        public long endSpan(long endEpochNanos) {
            synchronized (this) {
                if (childStartEpoch > 0) {
                    childDuration += (clock.now() - childStartEpoch);
                }
            }
            return endEpochNanos - startEpochNanos - childDuration;
        }
    }

}
