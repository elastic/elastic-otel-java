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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import java.lang.reflect.Field;
import java.util.logging.Logger;

public class ConfigLogger {
  private static final int DELAY_IN_SECONDS_AFTER_START_FOR_INITIAL_LOG;
  private static final Logger logger = Logger.getLogger(ConfigLogger.class.getName());
  private static volatile boolean firstLogged = false;

  static {
    String envDelay = System.getenv("ELASTIC_OTEL_JAVA_CONFIGURATION_LOG_DELAY_SECONDS");
    String propDelay = System.getProperty("elastic.otel.java.configuration.log.delay.seconds");
    int intDelay;
    if (propDelay != null && propDelay.length() > 0) {
      try {
        intDelay = Integer.parseInt(propDelay);
      } catch (Exception e) {
        intDelay = 30;
      }
    } else if (envDelay != null && envDelay.length() > 0) {
      try {
        intDelay = Integer.parseInt(envDelay);
      } catch (Exception e) {
        intDelay = 30;
      }
    } else {
      intDelay = 30;
    }
    DELAY_IN_SECONDS_AFTER_START_FOR_INITIAL_LOG = intDelay;
  }

  public static void logConfig() {
    if (firstLogged) {
      doLogConfig();
    }
  }

  private static void doLogConfig() {
    try {
      logger.info("GlobalOpenTelemetry: " + getLogConfigString());
    } catch (NoSuchFieldException | IllegalAccessException e) {
      firstLogged = false; // resetting so we only log the error once
      logger.warning(
          "Error getting 'delegate' from " + GlobalOpenTelemetry.get() + ": " + e.getMessage());
    }
  }

  static String getLogConfigString() throws NoSuchFieldException, IllegalAccessException {
    OpenTelemetry obfuscatedConfig = GlobalOpenTelemetry.get();
    Field config = obfuscatedConfig.getClass().getDeclaredField("delegate");
    config.setAccessible(true);
    return config.get(obfuscatedConfig).toString();
  }

  public static void triggerInitialLogConfig() {
    new java.util.Timer()
        .schedule(
            new java.util.TimerTask() {
              @Override
              public void run() {
                firstLogged = true;
                doLogConfig();
              }
            },
            1000 * DELAY_IN_SECONDS_AFTER_START_FOR_INITIAL_LOG);
  }
}
