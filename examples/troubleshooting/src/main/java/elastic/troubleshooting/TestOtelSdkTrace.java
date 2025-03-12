package elastic.troubleshooting;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

public class TestOtelSdkTrace {

  public static void main(String[] args) throws Exception {
    String servicename = args[0];
    String endpoint = args[1];
    String secretOrApikey = args[2];
    if (!endpoint.endsWith("/v1/traces")) {
      if (endpoint.endsWith("/")) {
        endpoint = endpoint + "v1/traces";
      } else {
        endpoint = endpoint + "/v1/traces";
      }
    }
    boolean isSecret = true;
    if (secretOrApikey.startsWith("secret:")) {
      secretOrApikey = secretOrApikey.substring(7);
    } else if (secretOrApikey.startsWith("apikey:")) {
      secretOrApikey = secretOrApikey.substring(7);
      isSecret = false;
    }

    System.out.println("Starting test ... initializing OpenTelemetry");

    Resource resource = Resource.getDefault().toBuilder().put("service.name", servicename).build();

    OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder().setEndpoint(endpoint)
        .addHeader("Authorization", (isSecret ? "Bearer " : "ApiKey ") + secretOrApikey).build();

    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build()).setResource(resource)
        .build();

    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider)
        .build();

    System.out.println("Starting test ... creating a span");

    Tracer tracer = openTelemetry.getTracer("TestOtelSdkTrace");
    Span span = tracer.spanBuilder("test span").startSpan();
    try {
      System.out.println("Starting test ... ending the span");
    } finally {
      span.end();
    }
    openTelemetry.shutdown().whenComplete(() -> {
      System.out.println("Ending test ... shutdown OpenTelemetry");
    });

    Thread.sleep(60_000L);
    System.out.println("Ending test ... terminating the JVM");
  }
}
