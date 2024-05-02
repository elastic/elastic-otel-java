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
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;

import io.opentelemetry.contrib.stacktrace.StackTraceSpanProcessor;

@AutoService(ChainingSpanProcessorAutoConfiguration.class)
public class SpanStackTraceProcessorAutoConfig implements ChainingSpanProcessorAutoConfiguration {

  static final String MIN_DURATION_CONFIG_OPTION = "elastic.otel.span.stack.trace.min.duration";

  @Override
  public void registerSpanProcessors(
      ConfigProperties properties, ChainingSpanProcessorRegisterer registerer) {

    Duration minDuration = properties.getDuration(MIN_DURATION_CONFIG_OPTION, Duration.ofMillis(5));
    registerer.register(
        next -> new StackTraceSpanProcessor(next, minDuration.toNanos(), span -> {
          return true; // TODO use real heuristic to avoid inferred spans
        }),
        ChainingSpanProcessorRegisterer.ORDER_FIRST);
  }
}
