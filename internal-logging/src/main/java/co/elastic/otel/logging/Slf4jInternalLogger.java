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

import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Internal logger implementation that delegates to SLF4J */
public class Slf4jInternalLogger implements InternalLogger {
  private final Logger logger;

  private Slf4jInternalLogger(String name) {
    this.logger = LoggerFactory.getLogger(name);
  }

  public static InternalLogger create(String name) {
    return new Slf4jInternalLogger(name);
  }

  @Override
  public boolean isLoggable(Level level) {
    switch (level) {
      case TRACE:
        return logger.isTraceEnabled();
      case DEBUG:
        return logger.isDebugEnabled();
      case INFO:
        return logger.isInfoEnabled();
      case WARN:
        return logger.isWarnEnabled();
      case ERROR:
        return logger.isErrorEnabled();
      default:
        throw new IllegalArgumentException("Unsupported level: " + level);
    }
  }

  @Override
  public void log(Level level, String s, @Nullable Throwable throwable) {
    logger.atLevel(toSlf4jLevel(level)).setCause(throwable).log(s);
  }

  private static org.slf4j.event.Level toSlf4jLevel(Level level) {
    switch (level) {
      case TRACE:
        return org.slf4j.event.Level.TRACE;
      case DEBUG:
        return org.slf4j.event.Level.DEBUG;
      case INFO:
        return org.slf4j.event.Level.INFO;
      case WARN:
        return org.slf4j.event.Level.WARN;
      case ERROR:
        return org.slf4j.event.Level.ERROR;
      default:
        throw new IllegalArgumentException("Unsupported level: " + level);
    }
  }

  @Override
  public String name() {
    return logger.getName();
  }
}
