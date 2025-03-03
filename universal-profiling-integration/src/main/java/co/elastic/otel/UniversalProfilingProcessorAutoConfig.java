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
import co.elastic.otel.common.config.PropertiesApplier;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.ResourceConfiguration;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(ChainingSpanProcessorAutoConfiguration.class)
public class UniversalProfilingProcessorAutoConfig
    implements ChainingSpanProcessorAutoConfiguration {

  private static final Logger logger =
      Logger.getLogger(UniversalProfilingProcessorAutoConfig.class.getName());

  static final String ENABLED_OPTION = "elastic.otel.universal.profiling.integration.enabled";
  static final String BUFFER_SIZE_OPTION =
      "elastic.otel.universal.profiling.integration.buffer.size";
  static final String SOCKET_DIR_OPTION = "elastic.otel.universal.profiling.integration.socket.dir";
  static final String VIRTUAL_THREAD_SUPPORT_OPTION =
      "elastic.otel.universal.profiling.integration.virtual.threads.enabled";

  private enum EnabledOptions {
    TRUE,
    FALSE,
    AUTO
  }

  @Override
  public void registerSpanProcessors(
      ConfigProperties properties, ChainingSpanProcessorRegisterer registerer) {

    String enabledDefault = EnabledOptions.AUTO.toString();
    String unsupportedReason = JvmtiAccess.getSystemUnsupportedReason();
    if (unsupportedReason != null) {
      logger.log(
          Level.FINE,
          "Default value for {0} is false, because the system is unsupported: {1}",
          new Object[] {ENABLED_OPTION, unsupportedReason});
      enabledDefault = EnabledOptions.FALSE.toString();
    }

    String enabledString = properties.getString(ENABLED_OPTION, enabledDefault).toUpperCase();
    EnabledOptions enabled = EnabledOptions.valueOf(enabledString);

    if (enabled == EnabledOptions.FALSE) {
      return;
    }
    Resource resource = ResourceConfiguration.createEnvironmentResource(properties);

    String serviceName = resource.getAttribute(ServiceAttributes.SERVICE_NAME);
    if (serviceName == null || serviceName.isEmpty()) {
      logger.warning(
          "Cannot start universal profiling integration because no service name was configured");
      return;
    }

    PropertiesApplier props = new PropertiesApplier(properties);
    registerer.register(
        next -> {
          try {
            UniversalProfilingProcessorBuilder builder =
                UniversalProfilingProcessor.builder(next, resource);
            builder.delayActivationAfterProfilerRegistration(enabled == EnabledOptions.AUTO);
            props.applyInt(BUFFER_SIZE_OPTION, builder::bufferSize);
            props.applyString(SOCKET_DIR_OPTION, builder::socketDir);
            props.applyBool(VIRTUAL_THREAD_SUPPORT_OPTION, builder::virtualThreadSupportEnabled);
            return builder.build();
          } catch (Exception e) {
            logger.log(
                Level.SEVERE,
                "Failed to initialize universal profiling integration, the feature won't work",
                e);
            return next;
          }
        });
  }
}
