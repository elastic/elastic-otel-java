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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class AgentLog {

  private static final String PATTERN = "%msg%n";

  private AgentLog() {}

  public static void init() {

    ConfigurationBuilder<BuiltConfiguration> conf =
        ConfigurationBuilderFactory.newConfigurationBuilder();

    //    conf.add(
    //        conf.newAppender("stdout", ConsoleAppender.PLUGIN_NAME)
    //            .add(conf.newLayout(PatternLayout.class.getName()))
    //            .addAttribute("pattern", PATTERN));

    //    conf.add(conf.newRootLogger().add(conf.newAppenderRef("stdout")));

    Configurator.initialize(conf.build());
  }

  /**
   * Sets the agent log level at runtime
   *
   * @param level log level
   */
  public static void setLevel(Level level) {
    // Using log4j2 implementation allows to change the log level programmatically at runtime
    // which is not directly possible through the slf4j API and simple implementation used in
    // upstream distribution

    Configurator.setAllLevels("", level);

    // when debugging we should avoid very chatty http client debug messages
    if (level.intLevel() >= Level.DEBUG.intLevel()) {
      Configurator.setLevel("okhttp3.internal.http2", Level.INFO);
      Configurator.setLevel("okhttp3.internal.concurrent.TaskRunner", Level.INFO);
    }
  }
}
