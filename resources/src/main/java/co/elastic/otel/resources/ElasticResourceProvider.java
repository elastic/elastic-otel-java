/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.resources;

import io.opentelemetry.contrib.aws.resource.BeanstalkResourceProvider;
import io.opentelemetry.contrib.aws.resource.Ec2ResourceProvider;
import io.opentelemetry.contrib.aws.resource.EcsResourceProvider;
import io.opentelemetry.contrib.aws.resource.EksResourceProvider;
import io.opentelemetry.contrib.aws.resource.LambdaResourceProvider;
import io.opentelemetry.contrib.resourceproviders.AppServerServiceNameProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElasticResourceProvider implements ResourceProvider {

  private static final Logger logger = Logger.getLogger(ElasticResourceProvider.class.getName());

  public static final String ASYNC_RESOURCE = "elastic.otel.async_resource";

  private ConfigProperties config;

  @Override
  public Resource createResource(ConfigProperties config) {
    this.config = config;
    if (config.getBoolean(ASYNC_RESOURCE, false)) {
      return getBaseResource(config);
    }
    return getBaseResource(config).merge(getExtraResource());
  }

  private Resource getBaseResource(ConfigProperties config) {
    // application server providers : file parsing only thus fast
    return invokeResourceProvider(new AppServerServiceNameProvider());
  }

  public Resource getExtraResource() {
    List<ResourceProvider> providers =
        Arrays.asList(
            // ec2 relies on http calls without pre-checks
            new Ec2ResourceProvider(),
            // beanstalk relies on json config file parsing
            new BeanstalkResourceProvider(),
            // relies on https call without pre-checks + TLS setup (thus quite expensive)
            new EksResourceProvider(),
            // relies on http call with url provided through env var used as pre-check
            new EcsResourceProvider(),
            // relies on env variables only
            new LambdaResourceProvider());
    Resource resource = Resource.empty();
    for (ResourceProvider provider : providers) {
      resource = resource.merge(invokeResourceProvider(provider));
    }
    return resource;
  }

  private Resource invokeResourceProvider(ResourceProvider provider) {
    try {
      return provider.createResource(config);
    } catch (RuntimeException e) {
      logger.warning(
          String.format("error while invoking resource provider: %s", provider.getClass()));
      return Resource.empty();
    } finally {
    }
  }
}
