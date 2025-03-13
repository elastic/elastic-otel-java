package elastic.troubleshooting;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;

public class TestOtelSdkMetrics {

  public static void main(String[] args) throws Exception {
    String servicename = args[0];
    String endpoint = args[1];
    String secretOrApikey = args[2];
    if (!endpoint.endsWith("/v1/metrics")) {
      if (endpoint.endsWith("/")) {
        endpoint = endpoint + "v1/metrics";
      } else {
        endpoint = endpoint + "/v1/metrics";
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

    OtlpHttpMetricExporter metricExporter = OtlpHttpMetricExporter.builder().setEndpoint(endpoint)
        .addHeader("Authorization", (isSecret ? "Bearer " : "ApiKey ") + secretOrApikey).build();

    MetricReader metricReader = PeriodicMetricReader.builder(metricExporter)
        .setInterval(3, TimeUnit.SECONDS)
        .build();

    SdkMeterProvider meterProvider = SdkMeterProvider.builder().registerMetricReader(metricReader)
        .setResource(resource).build();

    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(meterProvider)
        .build();

    System.out.println("Starting test ... creating metrics");

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

    openTelemetry.shutdown().whenComplete(() -> {
      System.out.println("Ending test ... shutdown OpenTelemetry");
    });

    Thread.sleep(60_000L);
    System.out.println("Ending test ... terminating the JVM");
  }
}
