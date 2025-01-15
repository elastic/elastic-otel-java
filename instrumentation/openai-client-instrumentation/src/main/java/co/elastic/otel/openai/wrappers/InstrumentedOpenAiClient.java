package co.elastic.otel.openai.wrappers;

import com.openai.client.OpenAIClient;
import io.opentelemetry.api.internal.ConfigUtil;

import java.lang.reflect.Method;
import java.net.URI;

public class InstrumentedOpenAiClient extends DelegatingInvocationHandler<OpenAIClient, InstrumentedOpenAiClient> {

    // Visible and non-final for testing
    InstrumentationSettings settings;

    private InstrumentedOpenAiClient(OpenAIClient delegate, InstrumentationSettings settings) {
        super(delegate);
        this.settings = settings;
    }

    @Override
    protected Class<OpenAIClient> getProxyType() {
        return OpenAIClient.class;
    }

    public static class Builder {

        private final OpenAIClient delegate;

        private boolean captureMessageContent = Boolean.valueOf(
                ConfigUtil.getString("otel.instrumentation.genai.capture.message.content", "false"));

        private boolean emitEvents = Boolean.valueOf(
                ConfigUtil.getString("elastic.otel.java.instrumentation.genai.emit.events", "" + captureMessageContent));

        private String baseUrl;

        private Builder(OpenAIClient delegate) {
            this.delegate = delegate;
        }

        public Builder captureMessageContent(boolean captureMessageContent) {
            this.captureMessageContent = captureMessageContent;
            return this;
        }

        public Builder emitEvents(boolean emitEvents) {
            this.emitEvents = emitEvents;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }


        public OpenAIClient build() {
            if (delegate instanceof InstrumentedOpenAiClient) {
                return delegate;
            }
            String hostname = null;
            Long port = null;
            if (baseUrl != null) {
                try {
                    URI parsed = new URI(baseUrl);
                    hostname = parsed.getHost();
                    int urlPort = parsed.getPort();
                    if (urlPort != -1) {
                        port = (long) urlPort;
                    } else {
                        long defaultPort = parsed.toURL().getDefaultPort();
                        if (defaultPort != -1) {
                            port = defaultPort;
                        }
                    }
                } catch (Exception e) {
                    // ignore malformed
                }
            }
            return new InstrumentedOpenAiClient(delegate, new InstrumentationSettings(emitEvents, captureMessageContent, hostname, port))
                    .createProxy();
        }
    }

    public static Builder wrap(OpenAIClient delegate) {
        return new Builder(delegate);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (methodName.equals("chat") && parameterTypes.length == 0) {
            return new InstrumentedChatService(delegate.chat(), settings).createProxy();
        }
        if (methodName.equals("embeddings") && parameterTypes.length == 0) {
            return new InstrumentedEmbeddingsService(delegate.embeddings(), settings);
        }
        return super.invoke(proxy, method, args);
    }

}
