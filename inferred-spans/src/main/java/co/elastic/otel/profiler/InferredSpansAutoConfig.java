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

import co.elastic.otel.profiler.config.WildcardMatcher;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class InferredSpansAutoConfig implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(InferredSpansAutoConfig.class.getName());

  static final String ENABLED_OPTION = "elastic.otel.inferred.spans.enabled";
  static final String LOGGING_OPTION = "elastic.otel.inferred.spans.logging.enabled";
  static final String DIAGNOSTIC_FILES_OPTION =
      "elastic.otel.inferred.spans.backup.diagnostic.files";
  static final String SAFEMODE_OPTION = "elastic.otel.inferred.spans.safe.mode";
  static final String POSTPROCESSING_OPTION = "elastic.otel.inferred.spans.post.processing.enabled";
  static final String SAMPLING_INTERVAL_OPTION = "elastic.otel.inferred.spans.sampling.interval";
  static final String MIN_DURATION_OPTION = "elastic.otel.inferred.spans.min.duration";
  static final String INCLUDED_CLASSES_OPTION = "elastic.otel.inferred.spans.included.classes";
  static final String EXCLUDED_CLASSES_OPTION = "elastic.otel.inferred.spans.excluded.classes";
  static final String INTERVAL_OPTION = "elastic.otel.inferred.spans.interval";
  static final String DURATION_OPTION = "elastic.otel.inferred.spans.duration";
  static final String LIB_DIRECTORY_OPTION = "elastic.otel.inferred.spans.lib.directory";

  @Override
  public void customize(AutoConfigurationCustomizer config) {
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

  private static class PropertiesApplier {

    private final ConfigProperties properties;

    private PropertiesApplier(ConfigProperties properties) {
      this.properties = properties;
    }

    void applyBool(String configKey, Consumer<Boolean> funcToApply) {
      applyValue(properties.getBoolean(configKey), funcToApply);
    }

    void applyInt(String configKey, Consumer<Integer> funcToApply) {
      applyValue(properties.getInt(configKey), funcToApply);
    }

    void applyDuration(String configKey, Consumer<Duration> funcToApply) {
      applyValue(properties.getDuration(configKey), funcToApply);
    }

    void applyString(String configKey, Consumer<String> funcToApply) {
      applyValue(properties.getString(configKey), funcToApply);
    }

    void applyWildcards(String configKey, Consumer<? super List<WildcardMatcher>> funcToApply) {
      String wildcardListString = properties.getString(configKey);
      if (wildcardListString != null && !wildcardListString.isEmpty()) {
        List<WildcardMatcher> values =
            Arrays.stream(wildcardListString.split(","))
                .filter(str -> !str.isEmpty())
                .map(WildcardMatcher::valueOf)
                .collect(Collectors.toList());
        if (!values.isEmpty()) {
          funcToApply.accept(values);
        }
      }
    }

    private static <T> void applyValue(T value, Consumer<T> funcToApply) {
      if (value != null) {
        funcToApply.accept(value);
      }
    }
  }
}
