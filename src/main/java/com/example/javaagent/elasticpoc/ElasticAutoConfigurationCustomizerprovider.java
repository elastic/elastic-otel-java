package com.example.javaagent.elasticpoc;

import com.google.auto.service.AutoService;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import java.util.Collections;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ElasticAutoConfigurationCustomizerprovider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {
        ElasticProfiler profiler = new ElasticProfiler();
        autoConfiguration.addTracerProviderCustomizer((sdkTracerProviderBuilder, configProperties) ->
                        sdkTracerProviderBuilder.addSpanProcessor(new ElasticSpanProcessor(profiler)))
                .addPropertiesCustomizer(configProperties -> {
                    ContextStorage.addWrapper(contextStorage -> new ElasticContextStorage(contextStorage, profiler));
                    return Collections.emptyMap();
                }).addSpanExporterCustomizer((spanExporter, configProperties) -> {
                    profiler.registerExporter(spanExporter);
                    return spanExporter;
                });
    }
}
