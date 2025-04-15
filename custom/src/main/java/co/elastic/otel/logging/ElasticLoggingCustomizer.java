package co.elastic.otel.logging;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.bootstrap.InternalLogger;
import io.opentelemetry.javaagent.tooling.LoggingCustomizer;
import io.opentelemetry.javaagent.tooling.config.EarlyInitAgentConfig;

@AutoService(LoggingCustomizer.class)
public class ElasticLoggingCustomizer implements LoggingCustomizer {

  @Override
  public String name() {
    // must match "otel.javaagent.logging" system property for SPI lookup
    return "elastic";
  }

  @Override
  public void init(EarlyInitAgentConfig earlyInitAgentConfig) {
    InternalLogger.initialize(Slf4jInternalLogger::create);
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
