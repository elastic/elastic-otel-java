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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableRuleBasedSamplerBuilder;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.CompositeSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingPredicate;
import io.opentelemetry.sdk.internal.IncludeExcludePredicate;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import io.opentelemetry.semconv.incubating.UserAgentIncubatingAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ElasticSampler implements Sampler {

  private static final Logger logger = Logger.getLogger(ElasticSampler.class.getName());

  public static ElasticSampler INSTANCE = new ElasticSampler();
  private static final Builder builder = new Builder();
  private static volatile Sampler delegate = builder.build();

  static final double DEFAULT_SAMPLE_RATIO = 1.0d;

  private ElasticSampler() {}

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  public Builder toBuilder() {
    return builder;
  }

  @Override
  public String getDescription() {
    return toString();
  }

  public String toString() {
    return delegate.getDescription();
  }

  public static class Builder {

    private List<String> ignoredUrlPatterns;
    private List<String> ignoredUserAgentPatterns;
    private double ratio;

    // package-private for testing
    Builder() {
      this.ignoredUrlPatterns = Collections.emptyList();
      this.ignoredUserAgentPatterns = Collections.emptyList();
      this.ratio = DEFAULT_SAMPLE_RATIO;
    }

    public Builder withProbability(double ratio) {
      this.ratio = ratio;
      return this;
    }

    public Builder withIgnoredUrlPatterns(List<String> patterns) {
      this.ignoredUrlPatterns = patterns;
      return this;
    }

    public Builder withIgnoredUserAgentPatterns(List<String> patterns) {
      this.ignoredUserAgentPatterns = patterns;
      return this;
    }

    // package-private for testing
    Sampler build() {
      int rulesCount = 0;
      ComposableRuleBasedSamplerBuilder ruleBuilder = ComposableSampler.ruleBasedBuilder();

      if (!ignoredUrlPatterns.isEmpty()) {
        Arrays.asList(
                UrlAttributes.URL_PATH, // stable
                UrlAttributes.URL_FULL, // stable
                AttributeKey.stringKey("http.url") // legacy (deprecated)
                )
            .forEach(
                key ->
                    ruleBuilder.add(
                        valueMatching(key, ignoredUrlPatterns), ComposableSampler.alwaysOff()));
        rulesCount++;
      }

      if (!ignoredUserAgentPatterns.isEmpty()) {
        Arrays.asList(
                UserAgentAttributes.USER_AGENT_ORIGINAL, // stable
                UserAgentIncubatingAttributes.USER_AGENT_NAME, // incubating
                AttributeKey.stringKey("http.user_agent") // legacy (deprecated)
                )
            .forEach(
                key ->
                    ruleBuilder.add(
                        valueMatching(key, ignoredUserAgentPatterns),
                        ComposableSampler.alwaysOff()));
        rulesCount++;
      }

      if (rulesCount == 0) {
        // no rules, just return the probability sampler directly
        return CompositeSampler.wrap(ComposableSampler.probability(ratio));
      }

      // probability sampler applied last without any attribute filtering
      ruleBuilder.add(any(), ComposableSampler.probability(ratio));
      return CompositeSampler.wrap(ruleBuilder.build());
    }

    public ElasticSampler buildAndSetGlobal() {
      Sampler sampler = build();
      logger.fine("set global sampler to " + sampler.getDescription());
      delegate = sampler;
      return INSTANCE;
    }

    private static SamplingPredicate any() {
      return (parentContext, traceId, name, spanKind, attributes, parentLinks) -> true;
    }

    private static SamplingPredicate valueMatching(
        AttributeKey<String> attributeKey, List<String> patterns) {
      Predicate<String> predicate = IncludeExcludePredicate.createPatternMatching(patterns, null);
      return new ValueMatchingSamplingPredicate(attributeKey, predicate);
    }

    private static class ValueMatchingSamplingPredicate implements SamplingPredicate {
      private final AttributeKey<String> attributeKey;
      private final Predicate<String> predicate;

      public ValueMatchingSamplingPredicate(
          AttributeKey<String> attributeKey, Predicate<String> predicate) {
        this.attributeKey = attributeKey;
        this.predicate = predicate;
      }

      @Override
      public boolean matches(
          Context parentContext,
          String traceId,
          String name,
          SpanKind spanKind,
          Attributes attributes,
          List<LinkData> parentLinks) {
        String value = attributes.get(attributeKey);
        if (value == null) {
          return false;
        }
        boolean result = predicate.test(value);
        if (logger.isLoggable(Level.FINE)) {
          // note: matching on a key means that the sampling intent will be applied,
          logger.log(
              Level.FINE,
              "matching on '" + attributeKey + "' with value '" + value + "' result: " + result);
        }

        return result;
      }

      @Override
      public String toString() {
        return "ValueMatchingSamplingPredicate{"
            + "attributeKey="
            + attributeKey
            + ", predicate="
            + predicate
            + '}';
      }
    }
  }
}
