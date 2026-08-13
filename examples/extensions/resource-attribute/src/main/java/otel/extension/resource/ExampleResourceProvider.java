package otel.extension.resource;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.DeploymentAttributes;

@AutoService(ResourceProvider.class)
public class ExampleResourceProvider implements ResourceProvider {

  @Override
  public Resource createResource(ConfigProperties config) {
    AttributesBuilder attributes = Attributes.builder();

    // automatically provide deployment environment name from custom environment variable
    String envName = System.getenv("ENVIRONMENT_NAME");
    if(envName!= null && !envName.isEmpty()){
      attributes.put(DeploymentAttributes.DEPLOYMENT_ENVIRONMENT_NAME, envName);
    }
    return Resource.create(attributes.build());
  }
}
