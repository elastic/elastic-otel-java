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
package co.elastic.otel.common.processor;

import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Function;

public interface ChainingSpanProcessorRegisterer {

  int ORDER_FIRST = Integer.MIN_VALUE;
  int ORDER_DEFAULT = 0;
  int ORDER_LAST = Integer.MAX_VALUE;

  default void register(Function<SpanProcessor, SpanProcessor> processorFactory) {
    register(processorFactory, ORDER_DEFAULT);
  }

  void register(Function<SpanProcessor, SpanProcessor> processorFactory, int order);
}
