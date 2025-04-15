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
    // TODO
    return true;
  }

  @Override
  public void log(Level level, String s, @Nullable Throwable throwable) {
    // TODO
  }

  @Override
  public String name() {
    return logger.getName();
  }
}
