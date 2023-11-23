package co.elastic.apm.otel.profiler;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.function.Consumer;

public class ProfilerTestSetup implements AutoCloseable {

  OpenTelemetrySdk sdk;

  SamplingProfiler profiler;

  InMemorySpanExporter spanExporter;


  public ProfilerTestSetup(OpenTelemetrySdk sdk, InferredSpansProcessor processor,
      InMemorySpanExporter spanExporter) {
    this.sdk = sdk;
    this.profiler = processor.profiler;
    this.spanExporter = spanExporter;
  }

  public List<SpanData> getSpans() {
    return spanExporter.getFinishedSpanItems();
  }

  @Override
  public void close() {
    sdk.close();
  }

  public static ProfilerTestSetup create(Consumer<InferredSpansProcessorBuilder> configCustomizer) {
    InferredSpansProcessorBuilder builder = InferredSpansConfiguration.builder();
    configCustomizer.accept(builder);

    InferredSpansProcessor processor = builder.build();

    InMemorySpanExporter exporter = InMemorySpanExporter.create();

    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(processor)
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build();
    processor.setTracerProvider(tracerProvider);

    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build();

    return new ProfilerTestSetup(sdk, processor, exporter);
  }

}
