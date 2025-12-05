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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.sampler.consistent56.ConsistentSampler;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

public enum ElasticSampler implements Sampler {
  INSTANCE;

  static final double DEFAULT_TRACEIDRATIO_SAMPLE_RATIO = 1.0d;

  private static volatile double latestRatio;
  private static volatile Sampler probabilitySampler =
      newProbabilitySampler(DEFAULT_TRACEIDRATIO_SAMPLE_RATIO);
  private static volatile Sampler filteringSampler = Sampler.alwaysOn();

  /**
   * Set the ratio of the probability sampler
   *
   * @param ratio sampling probability
   */
  public static void setRatio(double ratio) {
    probabilitySampler = newProbabilitySampler(ratio);
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    SamplingResult filterResult =
        filteringSampler.shouldSample(
            parentContext, traceId, name, spanKind, attributes, parentLinks);

    if (filterResult.getDecision() == SamplingDecision.DROP) {
      return filterResult;
    }

    return probabilitySampler.shouldSample(
        parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  /**
   * Configures the filtering of HTTP spans
   *
   * @param urlPatterns url regex patterns to drop
   * @param userAgentPatterns user-agent regex patterns to drop
   */
  public static void setFilterHttp(List<String> urlPatterns, List<String> userAgentPatterns) {
    filteringSampler = HttpFilteringSampler.create(urlPatterns, userAgentPatterns);
  }

  @Override
  public String getDescription() {
    return toString();
  }

  private static Sampler newProbabilitySampler(double ratio) {
    latestRatio = ratio;
    return ConsistentSampler.parentBased(ConsistentSampler.probabilityBased(ratio));
  }

  public String toString() {
    return "ElasticSampler(ratio=" + latestRatio + ", " + probabilitySampler.toString() + ")";
  }
}
