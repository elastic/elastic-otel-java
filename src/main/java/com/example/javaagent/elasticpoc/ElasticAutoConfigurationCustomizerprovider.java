package com.example.javaagent.elasticpoc;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

import java.util.Collections;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ElasticAutoConfigurationCustomizerprovider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {

        autoConfiguration.addTracerProviderCustomizer((sdkTracerProviderBuilder, configProperties) ->
                        // span processor registration
                        sdkTracerProviderBuilder.addSpanProcessor(new ElasticSpanProcessor(ElasticProfiler.INSTANCE)))
                .addPropertiesCustomizer(configProperties -> {
                    // Wrap context storage when configuration is loaded,
                    // configuration customization is used as an init hook but does not actually alters it.
                    ContextStorage.addWrapper(contextStorage -> new ElasticContextStorage(contextStorage, ElasticProfiler.INSTANCE));
                    return Collections.emptyMap();
                }).addSpanExporterCustomizer((spanExporter, configProperties) -> {
                    // reuse the existing exporter for inferred spans
                    ElasticProfiler.INSTANCE.registerExporter(spanExporter);
                    return spanExporter;
                });


    }
}
