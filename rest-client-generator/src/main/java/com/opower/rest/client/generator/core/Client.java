package com.opower.rest.client.generator.core;

import com.opower.rest.client.generator.extractors.DefaultEntityExtractorFactory;
import com.opower.rest.client.generator.util.IsHttpMethod;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Path;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class to make the return types of the inherited builders work correctly.
 * @author chris.phillips
 * @param <T> the type of the client to be created
 * @param <B> the type of the concrete builder
 */
public abstract class Client<T, B extends Client<T, B>> {

    protected ClientExecutor executor;
    protected ClientProviders clientProviders = new ClientProviders();
    protected List<ClientErrorInterceptor> clientErrorInterceptors;
    protected final ResourceInterface<T> resourceInterface;
    protected final UriProvider uriProvider;
    protected final ClassLoader loader;

    protected Client(ResourceInterface<T> resourceInterface, UriProvider uriProvider) {
        this.resourceInterface = checkNotNull(resourceInterface);
        this.uriProvider = checkNotNull(uriProvider);
        this.loader = checkNotNull(resourceInterface.getInterface().getClassLoader());
    }

    @SuppressWarnings("unchecked")
    public B clientErrorInterceptors(List<ClientErrorInterceptor> clientErrorInterceptors) {
        this.clientErrorInterceptors = checkNotNull(clientErrorInterceptors);
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B executor(ClientExecutor exec) {
        this.executor = exec;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B registerProviderInstance(Object provider) {
        this.clientProviders.registerProviderInstance(provider);
        return (B) this;
    }

    public T build() {
        if (this.executor == null)
            throw new IllegalArgumentException("You must provide a ClientExecutor");
        if (this.clientProviders == null)
            throw new IllegalArgumentException("can't have null Providers");

        final ProxyConfig config = new ProxyConfig(this.loader, this.executor, this.clientProviders, new DefaultEntityExtractorFactory(),
                                                   this.clientErrorInterceptors);
        return createProxy(this.resourceInterface.getInterface(), this.uriProvider, config);
    }

    @SuppressWarnings("unchecked")
    static <S> S createProxy(final Class<S> iface, UriProvider uriProvider, final ProxyConfig config) {
        HashMap<Method, MethodInvoker> methodMap = new HashMap<Method, MethodInvoker>();
        for (Method method : iface.getMethods()) {
            MethodInvoker invoker;
            Set<String> httpMethods = IsHttpMethod.getHttpMethods(method);
            if ((httpMethods == null || httpMethods.size() == 0) && method.isAnnotationPresent(Path.class) && method.getReturnType().isInterface()) {
                invoker = new SubResourceInvoker(uriProvider, method, config);
            } else {
                invoker = createClientInvoker(iface, method, uriProvider, config);
            }
            methodMap.put(method, invoker);
        }

        Class<?>[] intfs = { iface };

        ClientProxy clientProxy = new ClientProxy(methodMap, config);
        // this is done so that equals and hashCode work ok. Adding the rest to a
        // Collection will cause equals and hashCode to be invoked. The Spring
        // infrastructure had some problems without this.
        clientProxy.setClazz(iface);

        return (S) Proxy.newProxyInstance(config.getLoader(), intfs, clientProxy);
    }

    private static ClientInvoker createClientInvoker(Class<?> clazz, Method method, UriProvider uriProvider, ProxyConfig config) {
        Set<String> httpMethods = IsHttpMethod.getHttpMethods(method);
        if (httpMethods == null || httpMethods.size() != 1) {
            throw new RuntimeException("You must use at least one, but no more than one http method annotation on: " + method.toString());
        }
        ClientInvoker invoker = new ClientInvoker(uriProvider, clazz, method, config);
        invoker.setHttpMethod(httpMethods.iterator().next());
        return invoker;
    }

    /**
     * Basic JAX-RS client proxy builder.
     * @param <T> the type of the ResourceInterface to build a client for
     */
    public static final class Builder<T> extends Client<T, Builder<T>> {

        public Builder(ResourceInterface<T> resourceInterface, UriProvider uriProvider) {
            super(resourceInterface, uriProvider);
        }
    }
}
