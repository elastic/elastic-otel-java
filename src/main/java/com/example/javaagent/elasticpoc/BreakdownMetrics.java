package com.example.javaagent.elasticpoc;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class BreakdownMetrics {

    public static final BreakdownMetrics INSTANCE = new BreakdownMetrics();

    public BreakdownMetrics() {

    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {

    }

    public void onSpanStart(Context parentContext, ReadWriteSpan span) {

    }

    public void onSpanEnd(ReadableSpan span) {

    }


}
