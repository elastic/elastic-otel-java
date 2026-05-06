package co.elastic.otel.common;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.inferredspans.InferredSpansProcessor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InferredSpanDetectorTest {

  @Test
  public void inferredSpanTracerDetection() {
    test(InferredSpansProcessor.TRACER_NAME, true);
    test("test", false);
  }

  private void test(String tracerName, boolean expectedInferred){
    Tracer tracer =
        OpenTelemetrySdk.builder().build().getTracerProvider().tracerBuilder(tracerName).build();
    Span span = tracer.spanBuilder("span").startSpan();
    assertThat(InferredSpanDetector.isInferredSpan((ReadableSpan) span)).isEqualTo(expectedInferred);
  }

}
