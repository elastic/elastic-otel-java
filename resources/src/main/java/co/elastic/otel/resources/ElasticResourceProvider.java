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

import io.opentelemetry.contrib.aws.resource.BeanstalkResource;
import io.opentelemetry.contrib.aws.resource.Ec2Resource;
import io.opentelemetry.contrib.aws.resource.EcsResource;
import io.opentelemetry.contrib.aws.resource.EksResource;
import io.opentelemetry.contrib.aws.resource.LambdaResource;
import io.opentelemetry.contrib.resourceproviders.AppServerServiceNameProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

public class ElasticResourceProvider implements ResourceProvider {

  @Override
  public Resource createResource(ConfigProperties config) {
    Resource resource = Resource.empty();

    // TODO : find a way to make the resource providers async, and then retrieve it
    // maybe using a "magic value" for the config properties could be relevant here

    resource =
        resource
            // ec2 relies on http calls without pre-checks
            .merge(Ec2Resource.get())
            // beanstalk relies on json config file parsing
            .merge(BeanstalkResource.get())
            // relies on https call without pre-checks + TLS setup (thus quite expensive)
            .merge(EksResource.get())
            // relies on http call with url provided through env var used as pre-check
            .merge(EcsResource.get())
            // relies on env variables only
            .merge(LambdaResource.get());

    // application server providers
    resource = resource.merge(new AppServerServiceNameProvider().createResource(config));

    return resource;
  }
}
