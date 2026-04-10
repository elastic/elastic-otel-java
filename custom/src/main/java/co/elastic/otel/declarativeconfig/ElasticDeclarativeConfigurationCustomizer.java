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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectionModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ResourceModel;
import java.util.List;
import java.util.Set;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class ElasticDeclarativeConfigurationCustomizer
    implements DeclarativeConfigurationCustomizerProvider {

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          customizeResources(model);
          return model;
        });
  }

  private static void customizeResources(OpenTelemetryConfigurationModel model) {
    // this is equivalent to adding the following explicitly in declarative configuration
    //
    //  detection/development:
    //    detectors:
    //      - <... other detectors ...>
    //      - elastic_distribution:

    ResourceModel resource = model.getResource();
    if (resource == null) {
      resource = new ResourceModel();
      model.withResource(resource);
    }

    ExperimentalResourceDetectionModel detectionDevelopment = resource.getDetectionDevelopment();
    if (null == detectionDevelopment) {
      detectionDevelopment = new ExperimentalResourceDetectionModel();
      resource.withDetectionDevelopment(detectionDevelopment);
    }
    List<ExperimentalResourceDetectorModel> detectors =
        requireNonNull(detectionDevelopment.getDetectors());

    Set<String> names =
        detectors.stream()
            .flatMap(detector -> detector.getAdditionalProperties().keySet().stream())
            .collect(toSet());

    // add at the end to make it have priority over upstream distro provider (which is added 1st)
    if (!names.contains(ElasticDistroComponentProvider.NAME)) {
      ExperimentalResourceDetectorModel detector = new ExperimentalResourceDetectorModel();
      detector.getAdditionalProperties().put(ElasticDistroComponentProvider.NAME, null);
      detectors.add(detector);
    }
  }
}
