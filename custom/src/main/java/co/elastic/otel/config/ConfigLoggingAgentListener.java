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
package co.elastic.otel.config;

import co.elastic.otel.sampling.DynamicCompositeParentBasedTraceIdRatioBasedSampler;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.logging.Logger;

@AutoService(AgentListener.class)
public class ConfigLoggingAgentListener implements AgentListener {
  public static final String LOG_THE_CONFIG =
      "elastic.otel.java.experimental.configuration.logging.enabled";

  private static volatile boolean enableDynamicSamplingRate = false;

  private static final Logger logger = Logger.getLogger(ConfigLoggingAgentListener.class.getName());

  private static boolean logTheConfig = true;

  public static synchronized void logTheConfig(boolean logTheConfig) {
    ConfigLoggingAgentListener.logTheConfig = logTheConfig;
  }

  public static boolean getEnableDynamicSamplingRate() {
    return enableDynamicSamplingRate;
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    if (logTheConfig) {
      logger.info(autoConfiguredOpenTelemetrySdk.toString());
    }
    if (autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk().getSdkTracerProvider().getSampler()
        instanceof DynamicCompositeParentBasedTraceIdRatioBasedSampler) {
      enableDynamicSamplingRate = true;
    }
  }

  @Override
  public int order() {
    return 1;
  }
}
