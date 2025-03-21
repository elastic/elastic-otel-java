package elastic.troubleshooting;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException("exactly 4 arguments expected");
    }
    String signal = args[0];
    String serviceName = args[1];
    String endpoint = normalizeEndpoint(args[2], signal);
    String secretOrApikey = args[3];

    String authorizationHeader = null;
    if (secretOrApikey.startsWith("secret:")) {
      authorizationHeader = "Bearer " + secretOrApikey.substring(7);
    } else if (secretOrApikey.startsWith("apikey:")) {
      authorizationHeader = "ApiKey " + secretOrApikey.substring(7);
    }

    System.out.printf("Sending '%s' for service '%s' to endpoint '%s'\n", signal, serviceName,
        endpoint);

    switch (signal) {
      case "traces":
        testTraces(serviceName, endpoint, authorizationHeader);
        break;
      case "metrics":
        testMetrics(serviceName, endpoint, authorizationHeader);
        break;
      case "logs":
        testLogs(serviceName, endpoint, authorizationHeader);
        break;
      default:
        throw new IllegalArgumentException("unexpected signal " + signal);
    }

    Thread.sleep(60_000L);
    System.out.println("Ending test ... terminating the JVM");
  }

  private static void testTraces(String serviceName, String endpoint, String authorizationHeader) {
    System.out.println("Starting test ... initializing OpenTelemetry");

    Resource resource = Resource.getDefault().toBuilder().put("service.name", serviceName).build();

    OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
        .setEndpoint(endpoint)
        .addHeader("Authorization", authorizationHeader)
        .build();

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

    openTelemetry.shutdown()
        .whenComplete(() -> System.out.println("Ending test ... shutdown OpenTelemetry"));
  }

  private static void testMetrics(String serviceName, String endpoint, String authorizationHeader)
      throws InterruptedException {
    System.out.println("Starting test ... initializing OpenTelemetry");

    Resource resource = Resource.getDefault().toBuilder().put("service.name", serviceName).build();

    OtlpHttpMetricExporter metricExporter = OtlpHttpMetricExporter.builder()
        .setEndpoint(endpoint)
        .addHeader("Authorization", authorizationHeader).build();

    System.out.println("Starting test ... creating metrics");

    MetricReader metricReader = PeriodicMetricReader.builder(metricExporter)
        .setInterval(3, TimeUnit.SECONDS)
        .build();

    SdkMeterProvider meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader)
        .setResource(resource).build();

    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(meterProvider)
        .build();

    Meter meter = openTelemetry.getMeter("TestOtelSdktrace");
    LongCounter counter = meter.counterBuilder("jvm.thread.count").build();
    for (int i = 0; i < 100; i++) {
      counter.add(3);
      Thread.sleep(1000L);
    }
    for (int i = 0; i < 100; i++) {
      counter.add(0);
      Thread.sleep(1000L);
    }

    openTelemetry.shutdown()
        .whenComplete(() -> System.out.println("Ending test ... shutdown OpenTelemetry"));
  }

  private static void testLogs(String serviceName, String endpoint, String authorizationHeader)
      throws InterruptedException {

    System.out.println("Starting test ... initializing OpenTelemetry");

    Resource resource = Resource.getDefault().toBuilder()
        .put("service.name", serviceName)
        .build();

    OtlpHttpLogRecordExporter logExporter = OtlpHttpLogRecordExporter.builder()
        .setEndpoint(endpoint)
        .addHeader("Authorization", authorizationHeader)
        .build();

    SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
        .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
        .setResource(resource)
        .build();

    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
        .setLoggerProvider(loggerProvider)
        .build();

    System.out.println("Starting test ... creating logs");

    Logger logger = loggerProvider.loggerBuilder("test-logger").build();

    // note: here we are using directly the "internal API" for sending logs, which is not meant to be used
    // as-is in the applications as those rely on existing logging libraries like logback, slf4j or log4j
    // which then are instrumented by the OpenTelemetry instrumentation agent.
    //
    // This however provides the ability to send a few sample logs to the backend for testing without
    // having to use any logging library instrumentation
    for (int i = 1; i <= 10; i++) {
      logger.logRecordBuilder().setBody("log mesage " + i).emit();
      Thread.sleep(10);
    }

    openTelemetry.shutdown()
        .whenComplete(() -> System.out.println("Ending test ... shutdown OpenTelemetry"));

  }

  private static String normalizeEndpoint(String arg, String signal) {
    if (arg.endsWith("/v1/" + signal)) {
      return arg;
    }
    if (arg.endsWith("/")) {
      return arg + "v1/" + signal;
    } else {
      return arg + "/v1/" + signal;
    }
  }

}
