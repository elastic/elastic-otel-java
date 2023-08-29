package com.example.javaagent.elasticpoc;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class ElasticSpanProcessor implements SpanProcessor {

    private final ElasticProfiler profiler;

    public ElasticSpanProcessor(ElasticProfiler profiler) {
        this.profiler = profiler;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        profiler.onSpanStart(parentContext, span);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        profiler.onSpanEnd(span);
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    @Override
    public CompletableResultCode shutdown() {
        profiler.shutdown();
        return CompletableResultCode.ofSuccess();
    }
}
