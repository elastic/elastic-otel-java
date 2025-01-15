package co.elastic.otel.openai.wrappers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class DelegatingInvocationHandler<T, S extends DelegatingInvocationHandler<T, S>> implements InvocationHandler {

    private static final ClassLoader CLASS_LOADER = DelegatingInvocationHandler.class.getClassLoader();

    protected final T delegate;

    public DelegatingInvocationHandler(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    protected abstract Class<T> getProxyType();

    public T createProxy() {
        Class<T> proxyType = getProxyType();
        Object proxy = Proxy.newProxyInstance(CLASS_LOADER, new Class[]{proxyType}, this);
        return proxyType.cast(proxy);
    }
}
