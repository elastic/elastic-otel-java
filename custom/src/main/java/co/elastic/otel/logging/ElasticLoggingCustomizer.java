package co.elastic.otel.logging;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;
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

    // set debug logging when enabled through configuration to behave like the upstream distribution
    if (earlyConfig.getBoolean("otel.javaagent.debug", false)) {
      AgentLog.setLevel(InternalLogger.Level.DEBUG);
    } else {
      AgentLog.setLevel(InternalLogger.Level.INFO);
    }

  }

  @Override
  public void onStartupSuccess() {
  }

  @SuppressWarnings("CallToPrintStackTrace")
  @Override
  public void onStartupFailure(Throwable throwable) {
    throwable.printStackTrace();
  }
}
