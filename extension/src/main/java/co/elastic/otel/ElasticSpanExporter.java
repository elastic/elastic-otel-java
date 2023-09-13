package co.elastic.otel;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ElasticSpanExporter implements SpanExporter {

    private final SpanExporter delegate;

    private ConcurrentHashMap<SpanContext, ElasticBreakdownMetrics.SpanContextData> storage;

    public ElasticSpanExporter(SpanExporter delegate) {
        this.delegate = delegate;
        this.storage = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // shortcut in the rare case where no filtering is required
        if (storage.isEmpty()) {
            delegate.export(spans);
        }

        List<SpanData> toSend = new ArrayList<>(spans.size());
        for (SpanData span : spans) {
            SpanContext spanContext = span.getSpanContext();
            ElasticBreakdownMetrics.SpanContextData data = storage.remove(spanContext);
            if (data == null) {
                toSend.add(span);
            } else {
                long duration = span.getEndEpochNanos() - span.getStartEpochNanos();
                System.out.printf("span with self time %s total = %d, self = %d %n", span.getSpanId(), duration, data.getSelfTime());
                toSend.add(new DelegatingSpanData(span) {
                    @Override
                    public Attributes getAttributes() {
                        return span.getAttributes().toBuilder()
                                .put(ElasticAttributes.SELF_TIME_ATTRIBUTE, data.getSelfTime())
                                .put(ElasticAttributes.LOCAL_ROOT_NAME, data.getLocalRootSpanName())
                                .put(ElasticAttributes.LOCAL_ROOT_TYPE, data.getLocalRootSpanType())
                                .build();
                    }
                });
            }
        }

        return delegate.export(toSend);
    }

    public void report(SpanContext spanContext, ElasticBreakdownMetrics.SpanContextData data) {
        this.storage.put(spanContext, data);
    }

    @Override
    public CompletableResultCode flush() {
        storage.clear();
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

}
