package co.elastic.otel.openai.wrappers;

import com.openai.services.blocking.ChatService;

import java.lang.reflect.Method;

public class InstrumentedChatService extends DelegatingInvocationHandler<ChatService, InstrumentedChatService> {

    private final InstrumentationSettings settings;

    public InstrumentedChatService(ChatService delegate, InstrumentationSettings settings) {
        super(delegate);
        this.settings = settings;
    }

    @Override
    protected Class<ChatService> getProxyType() {
        return ChatService.class;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (methodName.equals("completions") && parameterTypes.length == 0) {
            return new InstrumentedChatCompletionService(delegate.completions(), settings);
        }
        return super.invoke(proxy, method, args);
    }

}
