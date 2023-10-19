package co.elastic.otel.resources;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

public class ElasticResourceProvider implements ResourceProvider {

  @Override
  public Resource createResource(ConfigProperties config) {

    return Resource.empty();
  }
}
