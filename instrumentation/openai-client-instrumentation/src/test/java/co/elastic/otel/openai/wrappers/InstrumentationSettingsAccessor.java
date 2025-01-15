package co.elastic.otel.openai.wrappers;

import com.openai.client.OpenAIClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class InstrumentationSettingsAccessor {

    public static void setEmitEvents(OpenAIClient client, boolean emitEvents) {
        InstrumentedOpenAiClient handler = (InstrumentedOpenAiClient) extractOpenAIClientHandler(client);

        InstrumentationSettings original = handler.settings;
        InstrumentationSettings updated = new InstrumentationSettings(
                emitEvents,
                original.captureMessageContent,
                original.serverAddress,
                original.serverPort
        );

        handler.settings = updated;
    }

    public static void setCaptureMessageContent(OpenAIClient client, boolean captureContent) {
        InstrumentedOpenAiClient handler = (InstrumentedOpenAiClient) extractOpenAIClientHandler(client);

        InstrumentationSettings original = handler.settings;
        InstrumentationSettings updated = new InstrumentationSettings(
                original.emitEvents,
                captureContent,
                original.serverAddress,
                original.serverPort
        );

        handler.settings = updated;
    }

    private static InvocationHandler extractOpenAIClientHandler(OpenAIClient client) {
        if (! (Proxy.isProxyClass(client.getClass()))) {
            throw new IllegalArgumentException("client is not a Proxy, but would be if instrumented");
        }

        return Proxy.getInvocationHandler(client);
    }

}
