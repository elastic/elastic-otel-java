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
package co.elastic.otel;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.javaagent.tooling.AgentVersion;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;

// TODO : add auto-service registration
public class ElasticDistroResourceProvider implements ResourceProvider {

  // TODO : we have to use those as long as the upstream otel agent hasn't been updated to 1.22.0+
  private static final AttributeKey<String> TELEMETRY_DISTRO_NAME =
      AttributeKey.stringKey("telemetry.distro.name");
  private static final AttributeKey<String> TELEMETRY_DISTRO_VERSION =
      AttributeKey.stringKey("telemetry.distro.version");

  @Override
  public Resource createResource(ConfigProperties configProperties) {
    return AgentVersion.VERSION == null
        ? Resource.empty()
        : Resource.create(
            Attributes.of(
                TELEMETRY_DISTRO_NAME, "elastic", TELEMETRY_DISTRO_VERSION, AgentVersion.VERSION));
  }
}
