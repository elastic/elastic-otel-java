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
package co.elastic.otel.dynamicconfig;

import co.elastic.opamp.client.CentralConfigurationManager;
import co.elastic.opamp.client.CentralConfigurationProcessor;
import co.elastic.otel.logging.AgentLog;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CentralConfig {
  private static final Logger logger = Logger.getLogger(CentralConfig.class.getName());

  static {
    DynamicConfigurationPropertyChecker.startCheckerThread();
  }

  public static void init(SdkTracerProviderBuilder providerBuilder, ConfigProperties properties) {
    String endpoint = properties.getString("elastic.otel.opamp.endpoint");
    logger.info("Enabling OpAMP as endpoint is defined: " + endpoint);
    if (endpoint == null || endpoint.isEmpty()) {
      return;
    }
    if (!endpoint.endsWith("v1/opamp")) {
      if (endpoint.endsWith("/")) {
        endpoint += "v1/opamp";
      } else {
        endpoint += "/v1/opamp";
      }
    }
    String serviceName = getServiceName(properties);
    logger.info("Starting OpAmp client for: " + serviceName + " on endpoint " + endpoint);
    DynamicInstrumentation.setTracerConfigurator(
        providerBuilder, DynamicConfiguration.UpdatableConfigurator.INSTANCE);
    CentralConfigurationManager centralConfigurationManager =
        CentralConfigurationManager.builder()
            .setServiceName(serviceName)
            .setPollingInterval(Duration.ofSeconds(30))
            .setConfigurationEndpoint(endpoint)
            .build();

    centralConfigurationManager.start(
        configuration -> {
          logger.fine("Received configuration: " + configuration);
          Configs.applyConfigurations(configuration);
          return CentralConfigurationProcessor.Result.SUCCESS;
        });

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("=========== Shutting down OpAmp client for: " + serviceName);
                  centralConfigurationManager.stop();
                }));
  }

  private static String getServiceName(ConfigProperties properties) {
    String serviceName = properties.getString("otel.service.name");
    if (serviceName != null) {
      return serviceName;
    }
    Map<String, String> resourceMap = properties.getMap("otel.resource.attributes");
    if (resourceMap != null) {
      serviceName = resourceMap.get("service.name");
      if (serviceName != null) {
        return serviceName;
      }
    }
    return "unknown_service:java"; // Specified default
  }

  public static class Configs {
    private static final Map<String, ConfigOption> configNameToConfig;
    private static final Set<String> currentNonDefaultConfigsApplied = new HashSet<>();

    static {
      configNameToConfig =
          Stream.of(
                  new SendLogs(),
                  new SendMetrics(),
                  new SendTraces(),
                  new DeactivateAllInstrumentations(),
                  new DeactivateInstrumentations(),
                  new LoggingLevel())
              .collect(Collectors.toMap(ConfigOption::getConfigName, option -> option));
    }

    public static synchronized void applyConfigurations(Map<String, String> configuration) {
      Set<String> copyOfCurrentNonDefaultConfigsApplied =
          new HashSet<>(currentNonDefaultConfigsApplied);
      configuration.forEach(
          (configurationName, configurationValue) -> {
            copyOfCurrentNonDefaultConfigsApplied.remove(configurationName);
            applyConfiguration(configurationName, configurationValue);
            currentNonDefaultConfigsApplied.add(configurationName);
          });
      if (!copyOfCurrentNonDefaultConfigsApplied.isEmpty()) {
        // We have configs that were applied previously but have now been set back to default and
        // have been removed from the configs being sent - so for all of these we need to set the
        // config back to default
        for (String configurationName : copyOfCurrentNonDefaultConfigsApplied) {
          applyDefaultConfiguration(configurationName);
          currentNonDefaultConfigsApplied.remove(configurationName);
        }
      }
    }

    public static void applyDefaultConfiguration(String configurationName) {
      configNameToConfig.get(configurationName).updateToDefault();
    }

    public static void applyConfiguration(String configurationName, String configurationValue) {
      if (configNameToConfig.containsKey(configurationName)) {
        configNameToConfig.get(configurationName).updateOrLog(configurationValue);
      } else {
        logger.warning(
            "Ignoring unknown confguration option: '"
                + configurationName
                + "' with value: "
                + configurationValue);
      }
    }
  }

  public abstract static class ConfigOption {
    protected final String configName;
    protected final String defaultConfigStringValue;

    protected ConfigOption(String configName1, String defaultConfigStringValue1) {
      configName = configName1;
      defaultConfigStringValue = defaultConfigStringValue1;
    }

    public String getConfigName() {
      return configName;
    }

    protected boolean getBoolean(String configurationValue) {
      String error =
          "'"
              + getConfigName()
              + "' configuration option can only be 'true' or 'false' but is: {0}";
      return getBoolean(configurationValue, error);
    }

    protected boolean getBoolean(String configurationValue, String error) {
      if ("true".equalsIgnoreCase(configurationValue)) {
        return true;
      } else if ("false".equalsIgnoreCase(configurationValue)) {
        return false;
      } else {
        throw new IllegalArgumentException(MessageFormat.format(error, configurationValue));
      }
    }

    public void updateOrLog(String configurationValue) {
      try {
        update(configurationValue);
      } catch (IllegalArgumentException e) {
        logger.warning(e.getMessage());
      }
    }

    abstract void update(String configurationValue) throws IllegalArgumentException;

    public void updateToDefault() {
      update(defaultConfigStringValue);
    }

    protected DynamicConfiguration config() {
      return DynamicConfiguration.getInstance();
    }
  }

  public static final class SendLogs extends ConfigOption {
    SendLogs() {
      super("send_logs", "true");
    }

    @Override
    void update(String configurationValue) throws IllegalArgumentException {
      config().setSendingLogs(getBoolean(configurationValue));
    }
  }

  public static final class SendMetrics extends ConfigOption {
    SendMetrics() {
      super("send_metrics", "true");
    }

    @Override
    void update(String configurationValue) throws IllegalArgumentException {
      config().setSendingMetrics(getBoolean(configurationValue));
    }
  }

  public static final class SendTraces extends ConfigOption {
    SendTraces() {
      super("send_traces", "true");
    }

    @Override
    void update(String configurationValue) throws IllegalArgumentException {
      config().setSendingSpans(getBoolean(configurationValue));
    }
  }

  public static final class DeactivateAllInstrumentations extends ConfigOption {
    DeactivateAllInstrumentations() {
      super("deactivate_all_instrumentations", "false");
    }

    @Override
    void update(String configurationValue) throws IllegalArgumentException {
      if (getBoolean(configurationValue)) {
        config().deactivateAllInstrumentations();
      } else {
        config().reactivateAllInstrumentations();
      }
    }
  }

  public static final class DeactivateInstrumentations extends ConfigOption {
    DeactivateInstrumentations() {
      super("deactivate_instrumentations", "");
    }

    @Override
    void update(String configurationValue) throws IllegalArgumentException {
      config().deactivateInstrumentations(configurationValue);
    }
  }

  public static final class LoggingLevel extends ConfigOption {
    LoggingLevel() {
      super("logging_level", "");
    }

    @Override
    void update(String configurationValue) throws IllegalArgumentException {
      AgentLog.setLevel(configurationValue);
    }
  }
}
