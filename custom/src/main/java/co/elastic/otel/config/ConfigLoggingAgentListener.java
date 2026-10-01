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

import co.elastic.otel.compositesampling.DynamicCompositeParentBasedTraceIdRatioBasedSampler;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@AutoService(AgentListener.class)
public class ConfigLoggingAgentListener implements AgentListener {
  public static final String LOG_THE_CONFIG =
      "elastic.otel.java.experimental.configuration.logging.enabled";

  private static final Logger logger = Logger.getLogger(ConfigLoggingAgentListener.class.getName());

  private static boolean logTheConfig = true;

  public static synchronized void logTheConfig(boolean logTheConfig) {
    ConfigLoggingAgentListener.logTheConfig = logTheConfig;
  }

  // Guarded by the class monitor. Null until afterAgent reports the tracer sampler.
  @Nullable private static Sampler sampler;

  // Guarded by the class monitor. A sampling rate received before the sampler was known.
  @Nullable private static Double pendingSamplingRate;

  /**
   * Applies a sampling rate to the dynamic sampler. A rate received before the tracer sampler is
   * initialized is held and applied once {@link #afterAgent} reports the sampler.
   */
  public static synchronized void updateSamplingRate(double ratio) {
    if (sampler == null) {
      logger.info("deferring \"sampling_rate\" until the tracer sampler is initialized");
      pendingSamplingRate = ratio;
      return;
    }
    applySamplingRate(ratio);
  }

  static synchronized void onSamplerInitialized(Sampler initializedSampler) {
    sampler = initializedSampler;
    if (pendingSamplingRate != null) {
      double ratio = pendingSamplingRate;
      pendingSamplingRate = null;
      applySamplingRate(ratio);
    }
  }

  // for testing only
  static synchronized void resetForTesting() {
    sampler = null;
    pendingSamplingRate = null;
  }

  private static void applySamplingRate(double ratio) {
    if (sampler instanceof DynamicCompositeParentBasedTraceIdRatioBasedSampler) {
      DynamicCompositeParentBasedTraceIdRatioBasedSampler.setRatio(ratio);
    } else {
      logger.warning(
          "ignoring \"sampling_rate\" because the configured sampler does not support dynamic"
              + " sampling: "
              + sampler.getDescription());
    }
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    if (logTheConfig) {
      logger.info(autoConfiguredOpenTelemetrySdk.toString());
    }
    onSamplerInitialized(
        autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk().getSdkTracerProvider().getSampler());
  }

  @Override
  public int order() {
    return 1;
  }
}
