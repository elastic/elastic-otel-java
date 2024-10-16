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

import co.elastic.otel.common.ChainingSpanProcessorAutoConfiguration;
import co.elastic.otel.common.ChainingSpanProcessorRegisterer;
import co.elastic.otel.common.ElasticAttributes;
import com.google.auto.service.AutoService;
import io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessor;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;

@AutoService(ChainingSpanProcessorAutoConfiguration.class)
public class SpanStackTraceProcessorAutoConfig implements ChainingSpanProcessorAutoConfiguration {

  static final String LEGACY_DURATION_CONFIG_OPTION =
      "elastic.otel.java.span-stacktrace.min.duration";
  // TODO replace this with upstream config once it's stable
  static final String MIN_DURATION_CONFIG_OPTION = "elastic.otel.java.span.stacktrace.min.duration";

  @Override
  public void registerSpanProcessors(
      ConfigProperties properties, ChainingSpanProcessorRegisterer registerer) {

    Duration legacyMinDuration =
        properties.getDuration(LEGACY_DURATION_CONFIG_OPTION, Duration.ofMillis(5));
    Duration minDuration = properties.getDuration(MIN_DURATION_CONFIG_OPTION, legacyMinDuration);
    if (minDuration.isNegative()) {
      return;
    }
    registerer.register(
        next ->
            new StackTraceSpanProcessor(
                next,
                minDuration.toNanos(),
                span -> {
                  // Do not add a stacktrace for inferred spans: If a good one was available
                  // it would have been added by the module creating this span
                  return !Boolean.TRUE.equals(span.getAttribute(ElasticAttributes.IS_INFERRED));
                }),
        ChainingSpanProcessorRegisterer.ORDER_FIRST);
  }
}
