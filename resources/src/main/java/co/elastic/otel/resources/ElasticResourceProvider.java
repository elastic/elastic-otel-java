package co.elastic.otel.resources;

import io.opentelemetry.contrib.aws.resource.Ec2Resource;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

import io.opentelemetry.contrib.aws.resource.BeanstalkResourceProvider;

public class ElasticResourceProvider implements ResourceProvider {

  @Override
  public Resource createResource(ConfigProperties config) {
    Resource resource = Resource.empty();

    resource = resource.merge(Ec2Resource.get());

    return resource;
  }
}
