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

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.otel.compositesampling.DynamicCompositeParentBasedTraceIdRatioBasedSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamicSamplingRateGateTest {

  private static final DynamicCompositeParentBasedTraceIdRatioBasedSampler DYNAMIC_SAMPLER =
      DynamicCompositeParentBasedTraceIdRatioBasedSampler.INSTANCE;

  @BeforeEach
  @AfterEach
  void reset() {
    ConfigLoggingAgentListener.resetForTesting();
    DynamicCompositeParentBasedTraceIdRatioBasedSampler.setRatio(1.0);
  }

  @Test
  void rateReceivedBeforeSamplerIsKnownIsAppliedOnInitialization() {
    ConfigLoggingAgentListener.updateSamplingRate(0.8);
    ConfigLoggingAgentListener.onSamplerInitialized(DYNAMIC_SAMPLER);

    assertThat(DYNAMIC_SAMPLER.toString()).contains("ratio=0.8");
  }

  @Test
  void rateIsIgnoredWhenConfiguredSamplerIsNotDynamic() {
    ConfigLoggingAgentListener.updateSamplingRate(0.8);
    ConfigLoggingAgentListener.onSamplerInitialized(Sampler.alwaysOn());

    assertThat(DYNAMIC_SAMPLER.toString()).contains("ratio=1.0");

    ConfigLoggingAgentListener.updateSamplingRate(0.5);

    assertThat(DYNAMIC_SAMPLER.toString()).contains("ratio=1.0");
  }

  @Test
  void rateReceivedAfterSamplerIsKnownIsAppliedDirectly() {
    ConfigLoggingAgentListener.onSamplerInitialized(DYNAMIC_SAMPLER);
    ConfigLoggingAgentListener.updateSamplingRate(0.3);

    assertThat(DYNAMIC_SAMPLER.toString()).contains("ratio=0.3");
  }

  @Test
  void defaultRateRestoresFullSamplingAfterDeferredRate() {
    ConfigLoggingAgentListener.updateSamplingRate(0.8);
    ConfigLoggingAgentListener.onSamplerInitialized(DYNAMIC_SAMPLER);
    assertThat(DYNAMIC_SAMPLER.toString()).contains("ratio=0.8");

    ConfigLoggingAgentListener.updateSamplingRate(1.0);

    assertThat(DYNAMIC_SAMPLER.toString()).contains("ratio=1.0");
  }
}
