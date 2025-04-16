package co.elastic.otel.logging;

import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nullable;

/**
 * Internal logger implementation that delegates to SLF4J
 */
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
