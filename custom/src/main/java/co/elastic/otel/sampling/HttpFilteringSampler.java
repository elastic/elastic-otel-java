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
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSamplerBuilder;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import java.util.List;

public class HttpFilteringSampler implements Sampler {

  private final Sampler delegate;

  private HttpFilteringSampler(Sampler delegate) {
    this.delegate = delegate;
  }

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

  @Override
  public String getDescription() {
    return delegate.getDescription();
  }

  public static HttpFilteringSampler create(
      List<String> urlPatterns, List<String> userAgentPatterns) {
    RuleBasedRoutingSamplerBuilder builder =
        RuleBasedRoutingSampler.builder(SpanKind.SERVER, Sampler.alwaysOn());
    for (String pattern : userAgentPatterns) {
      builder.drop(UserAgentAttributes.USER_AGENT_ORIGINAL, pattern);
    }
    for (String pattern : urlPatterns) {
      builder.drop(UrlAttributes.URL_PATH, pattern);
    }
    return new HttpFilteringSampler(builder.build());
  }

  @Override
  public String toString() {
    return "HttpFilteringSampler(delegate=" + delegate + ')';
  }
}
