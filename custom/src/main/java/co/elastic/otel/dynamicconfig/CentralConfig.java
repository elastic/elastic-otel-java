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

import co.elastic.otel.dynamicconfig.internal.OpampManager;
import co.elastic.otel.logging.AgentLog;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
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
    if (endpoint == null || endpoint.isEmpty()) {
      logger.fine("OpAMP is disabled");
      return;
    }
    logger.info("Enabling OpAMP as endpoint is defined: " + endpoint);
    if (!endpoint.endsWith("v1/opamp")) {
      if (endpoint.endsWith("/")) {
        endpoint += "v1/opamp";
      } else {
        endpoint += "/v1/opamp";
      }
    }
    String serviceName = getServiceName(properties);
    String environment = getServiceEnvironment(properties);
    logger.info("Starting OpAmp client for: " + serviceName + " on endpoint " + endpoint);
    DynamicInstrumentation.setTracerConfigurator(
        providerBuilder, DynamicConfiguration.UpdatableConfigurator.INSTANCE);
    OpampManager opampManager =
        OpampManager.builder()
            .setServiceName(serviceName)
            .setPollingInterval(Duration.ofSeconds(30))
            .setConfigurationEndpoint(endpoint)
            .setServiceEnvironment(environment)
            .build();

    opampManager.start(
        configuration -> {
          logger.fine("Received configuration: " + configuration);
          Configs.applyConfigurations(configuration, opampManager);
          return OpampManager.CentralConfigurationProcessor.Result.SUCCESS;
        });

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  logger.info("=========== Shutting down OpAMP client for: " + serviceName);
                  try {
                    opampManager.close();
                  } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error during OpAMP shutdown", e);
                  }
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

  private static String getServiceEnvironment(ConfigProperties properties) {
    Map<String, String> resourceMap = properties.getMap("otel.resource.attributes");
    if (resourceMap != null) {
      String environment = resourceMap.get("deployment.environment.name"); // semconv
      if (environment != null) {
        return environment;
      }
      return resourceMap.get("deployment.environment"); // backward compatible, can be null
    }
    return null;
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
                  new LoggingLevel(),
                  new PollingInterval())
              .collect(Collectors.toMap(ConfigOption::getConfigName, option -> option));
    }

    public static synchronized void applyConfigurations(
        Map<String, String> configuration, OpampManager opampManager) {
      Set<String> copyOfCurrentNonDefaultConfigsApplied =
          new HashSet<>(currentNonDefaultConfigsApplied);
      configuration.forEach(
          (configurationName, configurationValue) -> {
            copyOfCurrentNonDefaultConfigsApplied.remove(configurationName);
            applyConfiguration(configurationName, configurationValue, opampManager);
            currentNonDefaultConfigsApplied.add(configurationName);
          });
      if (!copyOfCurrentNonDefaultConfigsApplied.isEmpty()) {
        // We have configs that were applied previously but have now been set back to default and
        // have been removed from the configs being sent - so for all of these we need to set the
        // config back to default
        for (String configurationName : copyOfCurrentNonDefaultConfigsApplied) {
          applyDefaultConfiguration(configurationName, opampManager);
          currentNonDefaultConfigsApplied.remove(configurationName);
        }
      }
    }

    public static void applyDefaultConfiguration(
        String configurationName, OpampManager opampManager) {
      configNameToConfig.get(configurationName).updateToDefault(opampManager);
    }

    public static void applyConfiguration(
        String configurationName, String configurationValue, OpampManager opampManager) {
      if (configNameToConfig.containsKey(configurationName)) {
        configNameToConfig.get(configurationName).updateOrLog(configurationValue, opampManager);
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

    public void updateOrLog(String configurationValue, OpampManager opampManager) {
      try {
        update(configurationValue, opampManager);
      } catch (IllegalArgumentException e) {
        logger.warning(e.getMessage());
      }
    }

    abstract void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException;

    public void updateToDefault(OpampManager opampManager) {
      update(defaultConfigStringValue, opampManager);
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
    void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException {
      config().setSendingLogs(getBoolean(configurationValue));
    }
  }

  public static final class SendMetrics extends ConfigOption {
    SendMetrics() {
      super("send_metrics", "true");
    }

    @Override
    void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException {
      config().setSendingMetrics(getBoolean(configurationValue));
    }
  }

  public static final class SendTraces extends ConfigOption {
    SendTraces() {
      super("send_traces", "true");
    }

    @Override
    void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException {
      config().setSendingSpans(getBoolean(configurationValue));
    }
  }

  public static final class DeactivateAllInstrumentations extends ConfigOption {
    DeactivateAllInstrumentations() {
      super("deactivate_all_instrumentations", "false");
    }

    @Override
    void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException {
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
    void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException {
      config().deactivateInstrumentations(configurationValue);
    }
  }

  public static final class LoggingLevel extends ConfigOption {
    LoggingLevel() {
      super("logging_level", "");
    }

    @Override
    void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException {
      AgentLog.setLevel(configurationValue);
    }
  }

  public static final class PollingInterval extends ConfigOption {
    PollingInterval() {
      super("opamp_polling_interval", "30s");
    }

    @Override
    void update(String configurationValue, OpampManager opampManager)
        throws IllegalArgumentException {
      try {
        Duration duration = Duration.parse("PT" + configurationValue);
        opampManager.setPollingDelay(duration);
      } catch (DateTimeParseException e) {
        logger.warning(
            "Failed to update the polling interval, value passed was invalid: " + e.getMessage());
      }
    }
  }
}
