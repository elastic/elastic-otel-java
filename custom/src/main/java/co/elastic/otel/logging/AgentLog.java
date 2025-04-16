package co.elastic.otel.logging;

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
