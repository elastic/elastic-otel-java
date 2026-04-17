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
package co.elastic.otel.declarativeconfig;

import co.elastic.otel.ElasticDistroResource;
import com.google.auto.service.AutoService;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.resources.Resource;

/**
 * Provides {@code telemetry.distro.name} and {@code telemetry.distro.version} resource attributes
 * for declarative configuration
 */
@AutoService(ComponentProvider.class)
public class ElasticDistroComponentProvider implements ComponentProvider {

  static final String NAME = "elastic_distribution";

  @Override
  public Class<?> getType() {
    return Resource.class;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Object create(DeclarativeConfigProperties config) {
    return ElasticDistroResource.get();
  }
}
