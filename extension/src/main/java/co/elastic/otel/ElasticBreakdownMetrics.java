package co.elastic.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

import java.util.concurrent.ConcurrentHashMap;

public class ElasticBreakdownMetrics {


    private final VirtualField<SpanContext, TransactionData> transactionData;

    public static final ElasticBreakdownMetrics INSTANCE = new ElasticBreakdownMetrics();

    public ElasticBreakdownMetrics() {
        transactionData = VirtualField.find(SpanContext.class, TransactionData.class);
    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {

    }

    public void onSpanStart(Context parentContext, ReadWriteSpan span) {
        // here we can't use LocalRootSpan as it hasn't been added to the context when this is called
        // see io.opentelemetry.instrumentation.api.instrumenter.Instrumenter.doStart for details.
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        if (parentSpanContext.isValid() && !parentSpanContext.isRemote()) {
            // starting a regular span
        } else {
            // starting a transaction
            transactionData.set(span.getSpanContext(), new TransactionData());
            System.out.println("--> start transaction ");
        }

    }

    public void onSpanEnd(ReadableSpan span) {
        SpanContext parentSpanContext = span.getParentSpanContext();
        TransactionData transaction = transactionData.get(span.getSpanContext());
        transactionData.set(span.getSpanContext(), null);

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
