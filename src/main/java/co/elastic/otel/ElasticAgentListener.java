package co.elastic.otel;

import com.google.auto.service.AutoService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@AutoService(AgentListener.class)
public class ElasticAgentListener implements AgentListener {
    @Override
    public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
        // We have to use an AgentListener in order to properly access the global OpenTelemetry instance
        // trying to do this elsewhere can make attempting to call GlobalOpenTelemetry.set() more than once.
        //
        // Implementing this interface currently requires to add an explicit dependency to
        // 'opentelemetry-sdk-extension-autoconfigure' as it is not provided for now.
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();

        ElasticProfiler.INSTANCE.registerOpenTelemetry(openTelemetry);
        ElasticBreakdownMetrics.INSTANCE.registerOpenTelemetry(openTelemetry);

    }
}
