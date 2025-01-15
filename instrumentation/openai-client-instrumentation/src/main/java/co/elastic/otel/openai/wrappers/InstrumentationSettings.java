package co.elastic.otel.openai.wrappers;

import io.opentelemetry.api.common.AttributesBuilder;

import static co.elastic.otel.openai.wrappers.GenAiAttributes.SERVER_ADDRESS;
import static co.elastic.otel.openai.wrappers.GenAiAttributes.SERVER_PORT;

public class InstrumentationSettings {

    final boolean emitEvents;
    final boolean captureMessageContent;

    // we do not directly have access to the client baseUrl after construction, therefore we need to remember it
    final String serverAddress;
    final Long serverPort;

    InstrumentationSettings(boolean emitEvents, boolean captureMessageContent, String serverAddress, Long serverPort) {
        this.emitEvents = emitEvents;
        this.captureMessageContent = captureMessageContent;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void putServerInfoIntoAttributes(AttributesBuilder attributes) {
        if (serverAddress != null) {
            attributes.put(SERVER_ADDRESS, serverAddress);
        }
        if (serverPort != null) {
            attributes.put(SERVER_PORT, serverPort);
        }
    }
}
