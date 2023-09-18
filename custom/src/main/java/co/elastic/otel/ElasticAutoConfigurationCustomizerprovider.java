package co.elastic.otel;

import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import java.util.Collections;

public class ElasticAutoConfigurationCustomizerprovider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {

        autoConfiguration.addTracerProviderCustomizer((sdkTracerProviderBuilder, configProperties) ->
                        // span processor registration
                        sdkTracerProviderBuilder.addSpanProcessor(ElasticExtension.INSTANCE.getSpanProcessor()))
                .addPropertiesCustomizer(configProperties -> {
                    // Wrap context storage when configuration is loaded,
                    // configuration customization is used as an init hook but does not actually alter it.
                    ContextStorage.addWrapper(ElasticExtension.INSTANCE::wrapContextStorage);
                    return Collections.emptyMap();
                }).addSpanExporterCustomizer((spanExporter, configProperties) ->
                        // wrap the original span exporter
                        ElasticExtension.INSTANCE.wrapSpanExporter(spanExporter))
                .addMetricExporterCustomizer((exporter, config) -> new ElasticMetricExporter(exporter));


    }
}
