package co.elastic.otel;

import io.opentelemetry.api.common.AttributeKey;
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

    private ConcurrentHashMap<SpanContext, Long> selfTimeStorage;

    private static final AttributeKey<Long> key = AttributeKey.longKey("elastic.span.self_time");

    public ElasticSpanExporter(SpanExporter delegate) {
        this.delegate = delegate;
        this.selfTimeStorage = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // shortcut in the rare case where no filtering is required
        if (selfTimeStorage.isEmpty()) {
            delegate.export(spans);
        }

        List<SpanData> toSend = new ArrayList<>(spans.size());
        for (SpanData span : spans) {
            SpanContext spanContext = span.getSpanContext();
            Long selfTime = selfTimeStorage.remove(spanContext);
            if (selfTime == null) {
                toSend.add(span);
            } else {
                long duration = span.getEndEpochNanos() - span.getStartEpochNanos();
                System.out.printf("span with self time %s total = %d, self = %d %n", span.getSpanId(), duration, selfTime);
                toSend.add(new DelegatingSpanData(span) {
                    @Override
                    public Attributes getAttributes() {
                        return span.getAttributes().toBuilder().put(key, selfTime).build();
                    }
                });
            }
        }

        return delegate.export(toSend);
    }

    public void reportSelfTime(SpanContext spanContext, long value) {
        this.selfTimeStorage.put(spanContext, value);
    }

    @Override
    public CompletableResultCode flush() {
        selfTimeStorage.clear();
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

}
