package com.example.javaagent.elasticpoc;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;

import javax.annotation.Nullable;

public class ElasticContextStorage implements ContextStorage {

    private final ContextStorage delegate;
    private final ElasticProfiler profiler;

    ElasticContextStorage(ContextStorage delegate, ElasticProfiler profiler) {
        this.delegate = delegate;
        this.profiler = profiler;
    }

    @Override
    public Scope attach(Context toAttach) {
        // shortcut when context is already current
        if (Context.current() == toAttach) {
            return Scope.noop();
        }

        // shortcut when not recording or span already active
        Span span = Span.fromContext(toAttach);
        if (Span.current() == span || !span.isRecording()) {
            return delegate.attach(toAttach);
        }

        profiler.onSpanContextAttach(span);
        Scope delegatedScope = delegate.attach(toAttach);
        return () -> {
            delegatedScope.close();
            profiler.onSpanContextClose(span);
        };
    }

    @Nullable
    @Override
    public Context current() {
        return delegate.current();
    }
}
