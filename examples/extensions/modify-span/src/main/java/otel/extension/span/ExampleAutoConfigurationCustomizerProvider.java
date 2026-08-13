package otel.extension.span;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ExampleAutoConfigurationCustomizerProvider implements
    AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addTracerProviderCustomizer(this::configureSdkTracerProvider)
        .addSpanExporterCustomizer(this::configureSdkExporterCustomizer);
  }

  private SpanExporter configureSdkExporterCustomizer(SpanExporter spanExporter,
      ConfigProperties configProperties) {
    return new ExampleSpanExporter(spanExporter);
  }

  private SdkTracerProviderBuilder configureSdkTracerProvider(
      SdkTracerProviderBuilder tracerProvider, ConfigProperties config) {
    return tracerProvider.addSpanProcessor(new ExampleSpanProcessor());
  }
}
