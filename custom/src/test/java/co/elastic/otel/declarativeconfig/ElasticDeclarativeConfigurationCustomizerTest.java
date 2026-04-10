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
package co.elastic.otel.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.javaagent.tooling.resources.ResourceCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ElasticDeclarativeConfigurationCustomizerTest {

  // because declarative config relies on json mapping annotations, we can leverage this for testing
  // the configuration customization.
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void defaultConfig() {
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    model = applyConfigCustomize(model, new ElasticDeclarativeConfigurationCustomizer());

    // ensures that we add our resource detector even if the model does not provide any
    checkJson(
        model.getResource(),
        "{\"attributes\":[],\"detection/development\":{\"detectors\":[{\"elastic_distribution\":null}]}}");
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void upstreamProvider(boolean elasticFirst) {
    // upstream provider is always added first in the list, even if we add ours first
    // this ordering behavior is implemented in upstream provider
    OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel();
    DeclarativeConfigurationCustomizerProvider first;
    DeclarativeConfigurationCustomizerProvider second;
    if (elasticFirst) {
      first = new ElasticDeclarativeConfigurationCustomizer();
      second = new ResourceCustomizerProvider();
    } else {
      first = new ElasticDeclarativeConfigurationCustomizer();
      second = new ResourceCustomizerProvider();
    }

    model = applyConfigCustomize(model, first);
    model = applyConfigCustomize(model, second);
    checkJson(
        model.getResource(),
        "{\"attributes\":[],\"detection/development\":{\"detectors\":[{\"opentelemetry_javaagent_distribution\":null},{\"elastic_distribution\":null}]}}");
  }

  private void checkJson(Object o, String expected) {
    try {
      assertThat(objectMapper.writeValueAsString(o)).isEqualTo(expected);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private OpenTelemetryConfigurationModel applyConfigCustomize(
      OpenTelemetryConfigurationModel originalModel,
      DeclarativeConfigurationCustomizerProvider customizerProvider) {
    AtomicReference<OpenTelemetryConfigurationModel> resultModel = new AtomicReference<>();
    customizerProvider.customize(
        new TestCustomizer() {
          @Override
          public void addModelCustomizer(
              Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel>
                  customizer) {
            resultModel.set(customizer.apply(originalModel));
          }
        });
    return resultModel.get();
  }

  private static class TestCustomizer implements DeclarativeConfigurationCustomizer {

    @Override
    public void addModelCustomizer(
        Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel> customizer) {}

    @Override
    public <T extends SpanExporter> void addSpanExporterCustomizer(
        Class<T> exporterType, BiFunction<T, DeclarativeConfigProperties, T> customizer) {}

    @Override
    public <T extends MetricExporter> void addMetricExporterCustomizer(
        Class<T> exporterType, BiFunction<T, DeclarativeConfigProperties, T> customizer) {}

    @Override
    public <T extends LogRecordExporter> void addLogRecordExporterCustomizer(
        Class<T> exporterType, BiFunction<T, DeclarativeConfigProperties, T> customizer) {}
  }
}
