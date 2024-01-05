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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class ChainingSpanProcessorsInstaller implements AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
    List<ChainingSpanProcessorAutoConfiguration> autoConfigs = new ArrayList<>();
    ServiceLoader.load(ChainingSpanProcessorAutoConfiguration.class)
        .iterator()
        .forEachRemaining(autoConfigs::add);
    if (!autoConfigs.isEmpty()) {

      MutableCompositeSpanProcessor exporterProcessor = new MutableCompositeSpanProcessor();

      autoConfigurationCustomizer.addSpanProcessorCustomizer(
          (spanProcessor, config) -> {
            if (isSpanExportingProcessor(spanProcessor)) {
              if (exporterProcessor.isEmpty()) {
                exporterProcessor.addDelegate(spanProcessor);
                return createProcessorChain(autoConfigs, config, exporterProcessor);
              } else {
                exporterProcessor.addDelegate(spanProcessor);
                // return NOOP, because exporterProcessor is already registered
                return SpanProcessor.composite();
              }
            }
            return spanProcessor;
          });
    }
  }

  private SpanProcessor createProcessorChain(
      List<ChainingSpanProcessorAutoConfiguration> chainedProcessorAutoConfigs,
      ConfigProperties properties,
      MutableCompositeSpanProcessor terminalProcessor) {

    List<ProcessorFactoryWithOrder> factories = new ArrayList<>();

    for (ChainingSpanProcessorAutoConfiguration autoConfig : chainedProcessorAutoConfigs) {
      autoConfig.registerSpanProcessors(
          properties,
          new ChainingSpanProcessorRegisterer() {
            @Override
            public void register(
                Function<SpanProcessor, SpanProcessor> processorFactory, int order) {
              factories.add(new ProcessorFactoryWithOrder(processorFactory, order));
            }
          });
    }
    // sort from highest (= last processor) to first
    factories.sort((a, b) -> Integer.compare(b.order, a.order));

    SpanProcessor result = terminalProcessor;
    for (ProcessorFactoryWithOrder fac : factories) {
      result = fac.factory.apply(result);
    }
    return result;
  }

  private static class ProcessorFactoryWithOrder {

    public ProcessorFactoryWithOrder(Function<SpanProcessor, SpanProcessor> factory, int order) {
      this.factory = factory;
      this.order = order;
    }

    Function<SpanProcessor, SpanProcessor> factory;
    int order;
  }

  private boolean isSpanExportingProcessor(SpanProcessor spanProcessor) {
    return spanProcessor instanceof BatchSpanProcessor
        || spanProcessor instanceof SimpleSpanProcessor;
  }
}
