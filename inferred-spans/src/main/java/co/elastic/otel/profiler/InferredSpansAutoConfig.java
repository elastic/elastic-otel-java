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
package co.elastic.otel.profiler;

import co.elastic.otel.common.config.PropertiesApplier;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class InferredSpansAutoConfig implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(InferredSpansAutoConfig.class.getName());

  static final String ENABLED_OPTION = "otel.inferred.spans.enabled";
  static final String LOGGING_OPTION = "otel.inferred.spans.logging.enabled";
  static final String DIAGNOSTIC_FILES_OPTION = "otel.inferred.spans.backup.diagnostic.files";
  static final String SAFEMODE_OPTION = "otel.inferred.spans.safe.mode";
  static final String POSTPROCESSING_OPTION = "otel.inferred.spans.post.processing.enabled";
  static final String SAMPLING_INTERVAL_OPTION = "otel.inferred.spans.sampling.interval";
  static final String MIN_DURATION_OPTION = "otel.inferred.spans.min.duration";
  static final String INCLUDED_CLASSES_OPTION = "otel.inferred.spans.included.classes";
  static final String EXCLUDED_CLASSES_OPTION = "otel.inferred.spans.excluded.classes";
  static final String INTERVAL_OPTION = "otel.inferred.spans.interval";
  static final String DURATION_OPTION = "otel.inferred.spans.duration";
  static final String LIB_DIRECTORY_OPTION = "otel.inferred.spans.lib.directory";

  private static final Map<String, String> LEGACY_OPTIONS_MAP = new HashMap<>();

  static {
    LEGACY_OPTIONS_MAP.put("elastic." + ENABLED_OPTION, ENABLED_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + LOGGING_OPTION, LOGGING_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + DIAGNOSTIC_FILES_OPTION, DIAGNOSTIC_FILES_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + SAFEMODE_OPTION, SAFEMODE_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + POSTPROCESSING_OPTION, POSTPROCESSING_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + SAMPLING_INTERVAL_OPTION, SAMPLING_INTERVAL_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + MIN_DURATION_OPTION, MIN_DURATION_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + INCLUDED_CLASSES_OPTION, INCLUDED_CLASSES_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + EXCLUDED_CLASSES_OPTION, EXCLUDED_CLASSES_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + INTERVAL_OPTION, INTERVAL_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + DURATION_OPTION, DURATION_OPTION);
    LEGACY_OPTIONS_MAP.put("elastic." + LIB_DIRECTORY_OPTION, LIB_DIRECTORY_OPTION);
  }

  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addPropertiesCustomizer(
        props -> {
          Map<String, String> overrides = new HashMap<>();
          for (String oldKey : LEGACY_OPTIONS_MAP.keySet()) {
            String value = props.getString(oldKey);
            if (value != null) {
              String newKey = LEGACY_OPTIONS_MAP.get(oldKey);
              if (props.getString(newKey) == null) { // new value has not been configured
                log.log(
                    Level.WARNING,
                    "The configuration property {0} is deprecated, use {1} instead",
                    new Object[] {oldKey, newKey});
                overrides.put(newKey, value);
              }
            }
          }
          return overrides;
        });
    config.addTracerProviderCustomizer(
        (providerBuilder, properties) -> {
          if (properties.getBoolean(ENABLED_OPTION, false)) {
            InferredSpansProcessorBuilder builder = InferredSpansProcessor.builder();

            PropertiesApplier applier = new PropertiesApplier(properties);

            applier.applyBool(LOGGING_OPTION, builder::profilerLoggingEnabled);
            applier.applyBool(DIAGNOSTIC_FILES_OPTION, builder::backupDiagnosticFiles);
            applier.applyInt(SAFEMODE_OPTION, builder::asyncProfilerSafeMode);
            applier.applyBool(POSTPROCESSING_OPTION, builder::postProcessingEnabled);
            applier.applyDuration(SAMPLING_INTERVAL_OPTION, builder::samplingInterval);
            applier.applyDuration(MIN_DURATION_OPTION, builder::inferredSpansMinDuration);
            applier.applyWildcards(INCLUDED_CLASSES_OPTION, builder::includedClasses);
            applier.applyWildcards(EXCLUDED_CLASSES_OPTION, builder::excludedClasses);
            applier.applyDuration(INTERVAL_OPTION, builder::profilerInterval);
            applier.applyDuration(DURATION_OPTION, builder::profilingDuration);
            applier.applyString(LIB_DIRECTORY_OPTION, builder::profilerLibDirectory);

            providerBuilder.addSpanProcessor(builder.build());
          } else {
            log.finest(
                "Not enabling inferred spans processor because " + ENABLED_OPTION + " is not set");
          }
          return providerBuilder;
        });
  }
}
