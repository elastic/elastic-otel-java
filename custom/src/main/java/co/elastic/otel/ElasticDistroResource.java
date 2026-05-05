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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.javaagent.tooling.AgentVersion;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.TelemetryAttributes;

public class ElasticDistroResource {

  private ElasticDistroResource() {}

  public static Resource get() {
    if (AgentVersion.VERSION == null) {
      return Resource.empty();
    }
    try {
      Class.forName("co.elastic.otel.agent.ElasticAgent");
    } catch (ClassNotFoundException e) {
      // this means that we are running as an extension of the vanilla agent
      // and not as distro.
      return Resource.empty();
    }
    return Resource.create(
        Attributes.of(
            TelemetryAttributes.TELEMETRY_DISTRO_NAME,
            "elastic",
            TelemetryAttributes.TELEMETRY_DISTRO_VERSION,
            AgentVersion.VERSION));
  }
}
