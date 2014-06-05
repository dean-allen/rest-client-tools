package com.opower.rest.client.generator.core;

import com.opower.rest.client.generator.extractors.DefaultEntityExtractorFactory;
import com.opower.rest.client.generator.util.IsHttpMethod;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientBuilder<T> {

    public static <T> ClientBuilder<T> instance(ResourceInterface<T> resourceInterface, UriProvider uriProvider) throws Exception {
        return new ClientBuilder<T>(resourceInterface, uriProvider);
    }

    protected final ResourceInterface<T> resourceInterface;
    protected final UriProvider uriProvider;
    protected final ClassLoader loader;
    protected ClientExecutor executor;
    protected Providers providers;
    protected List<ClientErrorInterceptor> clientErrorInterceptors;

    protected ClientBuilder(ResourceInterface<T> resourceInterface, UriProvider uriProvider) {
        this.resourceInterface = checkNotNull(resourceInterface);
        this.uriProvider = checkNotNull(uriProvider);
        this.loader = checkNotNull(resourceInterface.getInterface().getClassLoader());
    }

    public ClientBuilder<T> clientErrorInterceptors(List<ClientErrorInterceptor> clientErrorInterceptors) {
        this.clientErrorInterceptors = checkNotNull(clientErrorInterceptors);
        return this;
    }

    public ClientBuilder<T> executor(ClientExecutor exec) {
        this.executor = exec;
        return this;
    }

    public ClientBuilder<T> messageBodyProviders(MessageBodyReader messageBodyReader, MessageBodyWriter messageBodyWriter) {
        this.providers = buildProviders(messageBodyReader, messageBodyWriter);
        return this;
    }

    public T build() {
        if (this.executor == null)
            throw new IllegalArgumentException("You must provide a ClientExecutor");
        if (this.providers == null)
            throw new IllegalArgumentException("can't have null Providers");

        final ProxyConfig config = new ProxyConfig(this.loader, this.executor, this.providers, new DefaultEntityExtractorFactory(),
                this.clientErrorInterceptors);
        return createProxy(this.resourceInterface.getInterface(), this.uriProvider, config);
    }

    private Providers buildProviders(final MessageBodyReader messageBodyReader, final MessageBodyWriter messageBodyWriter) {
        return  new Providers() {
            @Override
            public <MBR> MessageBodyReader<MBR> getMessageBodyReader(Class<MBR> tClass, Type type, Annotation[] annotations, MediaType mediaType) {
                return messageBodyReader.isReadable(tClass, type, annotations, mediaType) ? messageBodyReader : null;
            }

            @Override
            public <MBW> MessageBodyWriter<MBW> getMessageBodyWriter(Class<MBW> tClass, Type type, Annotation[] annotations, MediaType mediaType) {
                return messageBodyWriter.isWriteable(tClass, type, annotations, mediaType) ? messageBodyWriter : null;
            }

            @Override
            public <EM extends Throwable> ExceptionMapper<EM> getExceptionMapper(Class<EM> tClass) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <R> ContextResolver<R> getContextResolver(Class<R> tClass, MediaType mediaType) {
                throw new UnsupportedOperationException();
            }
        };
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

}
