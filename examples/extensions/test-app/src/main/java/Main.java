import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.time.Instant;

public class Main {

  private static final int SPAN_COUNT = 5;

  private static final Tracer tracer = GlobalOpenTelemetry.get().getTracer("example-tracer");

  public static void main(String[] args) {

    highCardinalitySpans();

    httpHealthCheckSpans();

  }

  private static void httpHealthCheckSpans() {
    Instant now = Instant.now();
    for (int i = 0; i < SPAN_COUNT; i++) {
      String name = "/healthcheck";
      log("creating health check server span with name: " + name);
      Span span = tracer.spanBuilder(name)
          // emulate an http server endpoint span by using semconv attributes
          .setSpanKind(SpanKind.SERVER)
          .setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, "GET")
          .setAttribute(UrlAttributes.URL_PATH, "/healthcheck")
          .setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200)
          //
              .setStartTimestamp(now.plusMillis(SPAN_COUNT *i))
              .startSpan();
      span.end(now.plusMillis(SPAN_COUNT *(i+1)));
    }
  }


  private static void highCardinalitySpans() {
    Instant now = Instant.now();
    for (int i = 0; i < SPAN_COUNT; i++) {
      String name = String.format("0x0%d", i);
      log("creating high cardinality client span with name: " + name);
      Span span = tracer.spanBuilder(name)
          .setSpanKind(SpanKind.CLIENT)
              .setStartTimestamp(now.plusMillis(SPAN_COUNT *i))
              .startSpan();
      span.end(now.plusMillis(SPAN_COUNT *(i+1)));
    }

  }

  private static void log(String msg) {
    System.out.println(">>> " + msg);
  }

}
