package co.elastic.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class ElasticExtension {

    public static final ElasticExtension INSTANCE = new ElasticExtension();

    private final ElasticProfiler profiler;
    private final ElasticBreakdownMetrics breakdownMetrics;
    private final ElasticSpanProcessor spanProcessor;
    private ElasticSpanExporter spanExporter;

    private ElasticExtension() {
        this.profiler = new ElasticProfiler();
        this.breakdownMetrics = new ElasticBreakdownMetrics();
        this.spanProcessor = new ElasticSpanProcessor(profiler, breakdownMetrics);
    }

    public void registerOpenTelemetry(OpenTelemetry openTelemetry) {
        profiler.registerOpenTelemetry(openTelemetry);
        breakdownMetrics.registerOpenTelemetry(openTelemetry);
    }

    public SpanProcessor getSpanProcessor() {
        return spanProcessor;
    }

    public ContextStorage wrapContextStorage(ContextStorage toWrap) {
        return new ElasticContextStorage(toWrap, profiler);
    }

    public SpanExporter wrapSpanExporter(SpanExporter toWrap) {
        // make the sampling profiler directly use the original exporter
        profiler.registerExporter(toWrap);
        spanExporter = new ElasticSpanExporter(toWrap);
        breakdownMetrics.registerSpanExporter(spanExporter);
        return spanExporter;
    }

}
