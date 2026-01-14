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
package co.elastic.otel.sampling;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@AutoService(ConfigurableSamplerProvider.class)
public class ElasticSamplerProvider implements ConfigurableSamplerProvider {

  private static final String ELASTIC_OTEL_IGNORE_URLS =
      "elastic.otel.experimental.http.ignore.urls";
  private static final String ELASTIC_OTEL_IGNORE_USER_AGENTS =
      "elastic.otel.experimental.http.ignore.user-agents";

  @Override
  public Sampler createSampler(ConfigProperties config) {
    return ElasticSampler.globalBuilder()
        .withProbability(config.getDouble(
            "otel.traces.sampler.arg", ElasticSampler.DEFAULT_SAMPLE_RATIO))
        .withIgnoredUrlPatterns(config.getList(ELASTIC_OTEL_IGNORE_URLS))
        .withIgnoredUserAgentPatterns(config.getList(ELASTIC_OTEL_IGNORE_USER_AGENTS))
        .buildAndSetGlobal();
  }

  @Override
  public String getName() {
    return "elastic";
  }
}
