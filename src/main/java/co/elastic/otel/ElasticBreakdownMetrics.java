package co.elastic.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

import java.util.concurrent.ConcurrentHashMap;

public class ElasticBreakdownMetrics {

    private final ConcurrentHashMap<SpanContext, TransactionData> transactionData;

    public static final ElasticBreakdownMetrics INSTANCE = new ElasticBreakdownMetrics();

    public ElasticBreakdownMetrics() {
        transactionData = new ConcurrentHashMap<>();
    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {

    }

    public void onSpanStart(Context parentContext, ReadWriteSpan span) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        if (parentSpanContext.isValid() && !parentSpanContext.isRemote()) {
            // starting a regular span
        } else {
            // starting a transaction
            transactionData.put(span.getSpanContext(), new TransactionData());
            System.out.println("--> start transaction ");
        }


    }

    public void onSpanEnd(ReadableSpan span) {
        SpanContext parentSpanContext = span.getParentSpanContext();
        TransactionData transaction = transactionData.remove(span.getSpanContext());

        if (null == transaction) {
            // ended span is not a transaction, but might have a transaction parent
            if (parentSpanContext.isValid()) {
                transactionData.get(parentSpanContext);
            }

        } else {
            System.out.println("--> end transaction ");
        }

    }

    private static class TransactionData {

    }


}
