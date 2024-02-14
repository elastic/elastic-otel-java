package co.elastic.otel;

import co.elastic.otel.common.ChainingSpanProcessorAutoConfiguration;
import co.elastic.otel.common.ChainingSpanProcessorRegisterer;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.ResourceConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

@AutoService(ChainingSpanProcessorAutoConfiguration.class)
public class UniversalProfilingProcessorAutoConfig implements
    ChainingSpanProcessorAutoConfiguration {
  @Override
  public void registerSpanProcessors(ConfigProperties properties,
      ChainingSpanProcessorRegisterer registerer) {
    Resource resource = ResourceConfiguration.createEnvironmentResource(properties);
    registerer.register(next -> UniversalProfilingProcessor.builder(next, resource).build());

  }
}
