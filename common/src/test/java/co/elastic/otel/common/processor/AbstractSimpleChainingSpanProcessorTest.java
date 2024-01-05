package co.elastic.otel.common.processor;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class AbstractSimpleChainingSpanProcessorTest {

  private InMemorySpanExporter spans;
  private SpanProcessor exportProcessor;

  @BeforeEach
  public void setup() {
    spans = InMemorySpanExporter.create();
    exportProcessor = SimpleSpanProcessor.create(spans);
  }

  @Test
  public void testSpanDropping() {
    SpanProcessor processor = new AbstractSimpleChainingSpanProcessor(exportProcessor) {
      @Override
      protected ReadableSpan doOnEnd(ReadableSpan readableSpan) {
        if (readableSpan.getName().startsWith("dropMe")) {
          return null;
        } else {
          return readableSpan;
        }
      }
    };
    try (OpenTelemetrySdk sdk = sdkWith(processor)) {
      Tracer tracer = sdk.getTracer("dummy-tracer");

      tracer.spanBuilder("dropMe1").startSpan().end();
      tracer.spanBuilder("sendMe").startSpan().end();
      tracer.spanBuilder("dropMe2").startSpan().end();

      assertThat(spans.getFinishedSpanItems())
          .hasSize(1)
          .anySatisfy(span -> assertThat(span).hasName("sendMe"));
    }
  }

  private OpenTelemetrySdk sdkWith(SpanProcessor processor) {
    return OpenTelemetrySdk.builder()
        .setTracerProvider(SdkTracerProvider.builder()
            .addSpanProcessor(processor)
            .build())
        .build();
  }

}
