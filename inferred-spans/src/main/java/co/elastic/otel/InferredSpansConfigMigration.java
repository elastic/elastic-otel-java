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

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class InferredSpansConfigMigration implements AutoConfigurationCustomizerProvider {

  private static final Logger log = Logger.getLogger(InferredSpansConfigMigration.class.getName());

  private static final Map<String, String> CONFIG_MAPPING = new HashMap<>();

  static {
    CONFIG_MAPPING.put("elastic.otel.inferred.spans.enabled", "otel.inferred.spans.enabled");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.logging.enabled", "otel.inferred.spans.logging.enabled");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.backup.diagnostic.files",
        "otel.inferred.spans.backup.diagnostic.files");
    CONFIG_MAPPING.put("elastic.otel.inferred.spans.safe.mode", "otel.inferred.spans.safe.mode");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.post.processing.enabled",
        "otel.inferred.spans.post.processing.enabled");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.sampling.interval", "otel.inferred.spans.sampling.interval");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.min.duration", "otel.inferred.spans.min.duration");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.included.classes", "otel.inferred.spans.included.classes");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.excluded.classes", "otel.inferred.spans.excluded.classes");
    CONFIG_MAPPING.put("elastic.otel.inferred.spans.interval", "otel.inferred.spans.interval");
    CONFIG_MAPPING.put("elastic.otel.inferred.spans.duration", "otel.inferred.spans.duration");
    CONFIG_MAPPING.put(
        "elastic.otel.inferred.spans.lib.directory", "otel.inferred.spans.lib.directory");
  }

  @Override
  public void customize(AutoConfigurationCustomizer config) {
    config.addPropertiesCustomizer(
        props -> {
          Map<String, String> overrides = new HashMap<>();
          for (String oldKey : CONFIG_MAPPING.keySet()) {
            String value = props.getString(oldKey);
            if (value != null) {
              String newKey = CONFIG_MAPPING.get(oldKey);
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
  }
}
