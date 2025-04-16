package co.elastic.otel.logging;

import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class AgentLog {

  private AgentLog() {

  }

  /**
   * Sets the agent log level at runtime
   *
   * @param level log level
   */
  public static void setLevel(InternalLogger.Level level) {
    // Using log4j2 implementation allows to change the log level programmatically at runtime
    // which is not directly possible through the slf4j API and simple implementation used in
    // upstream distribution

    Level rootLevel = toLog4jLevel(level);
    Configurator.setRootLevel(rootLevel);

    // when debugging we should avoid very chatty http client debug messages
    if (rootLevel.intLevel() >= Level.DEBUG.intLevel()) {
      Configurator.setLevel("okhttp3.internal.http2", Level.INFO);
      Configurator.setLevel("okhttp3.internal.concurrent.TaskRunner", Level.INFO);
    }
  }

  private static Level toLog4jLevel(InternalLogger.Level level) {
    switch (level) {
      case TRACE:
        return Level.TRACE;
      case DEBUG:
        return Level.DEBUG;
      case INFO:
        return Level.INFO;
      case WARN:
        return Level.WARN;
      case ERROR:
        return Level.ERROR;
      default:
        throw new IllegalArgumentException("Unsupported level: " + level);
    }
  }
}
