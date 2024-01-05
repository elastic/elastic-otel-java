package co.elastic.otel.common.processor;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

public interface ChainingSpanProcessorAutoConfiguration {

  void registerSpanProcessors(ConfigProperties properties,
      ChainingSpanProcessorRegisterer registerer);
}
