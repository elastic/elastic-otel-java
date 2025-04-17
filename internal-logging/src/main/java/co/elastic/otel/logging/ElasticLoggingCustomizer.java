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
package co.elastic.otel.logging;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;
import org.apache.logging.log4j.Level;
import org.slf4j.LoggerFactory;

@AutoService(LoggingCustomizer.class)
public class ElasticLoggingCustomizer implements LoggingCustomizer {

  @Override
  public String name() {
    // must match "otel.javaagent.logging" system property for SPI lookup
    return "elastic";
  }

  @Override
  public void init(EarlyInitAgentConfig earlyConfig) {

    // trigger loading the slf4j provider from the agent CL, this should load log4j implementation
    LoggerFactory.getILoggerFactory();

    // make the agent internal logger delegate to slf4j, which will delegate to log4j
    InternalLogger.initialize(Slf4jInternalLogger::create);

    AgentLog.init();

    Level level = null;
    if (earlyConfig.getBoolean("otel.javaagent.debug", false)) {
      // set debug logging when enabled through configuration to behave like the upstream
      // distribution
      level = Level.DEBUG;
    } else {
      String levelConfig = earlyConfig.getString("elastic.otel.javaagent.log.level");
      if (levelConfig != null) {
        level = Level.getLevel(levelConfig);
      }
    }
    AgentLog.setLevel(level != null ? level : Level.INFO);
  }

  @Override
  public void onStartupSuccess() {}

  @SuppressWarnings("CallToPrintStackTrace")
  @Override
  public void onStartupFailure(Throwable throwable) {
    throwable.printStackTrace();
  }
}
