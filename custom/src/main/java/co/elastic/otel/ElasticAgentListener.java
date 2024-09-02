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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

// @AutoService(AgentListener.class)
@Deprecated
public class ElasticAgentListener implements AgentListener {
  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    // We have to use an AgentListener in order to properly access the global OpenTelemetry instance
    // trying to do this elsewhere can make attempting to call GlobalOpenTelemetry.set() more than
    // once.
    //
    // Implementing this interface currently requires to add an explicit dependency to
    // 'opentelemetry-sdk-extension-autoconfigure' as it is not provided for now.
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();

    ElasticExtension.INSTANCE.registerOpenTelemetry(openTelemetry);
  }
}
