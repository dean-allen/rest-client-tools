package com.opower.rest.client.generator.core;


import com.google.common.base.Throwables;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

public class SubResourceInvoker implements MethodInvoker {
    final ProxyConfig config;
    final Class<?> iface;
    final UriProvider baseProvider;
    final String format;

    public SubResourceInvoker(UriProvider uriProvider, Method method, ProxyConfig config) {
        this.baseProvider = uriProvider;
        this.iface = method.getReturnType();
        this.config = config;
        String path = method.getAnnotation(Path.class).value();
        if (path.startsWith("/"))
            path = path.substring(1);
        Annotation[][] params = method.getParameterAnnotations();
        int index = 1;
        for (Annotation[] param : params) {
            for (Annotation a : param) {
                if (a instanceof PathParam) {
                    String name = ((PathParam) a).value();
                    path = path.replace("{" + name + "}", "%" + index + "$s");
                    break;
                }
            }
            index++;
        }
        this.format = path;
    }

    @Override
    public Object invoke(Object[] args) {
        String path = String.format(format, args);
        return Client.createProxy(iface, new SubResourceUriProvider(path, this.baseProvider), config);
    }

    public static class SubResourceUriProvider implements UriProvider {

        private final String path;
        private final UriProvider baseProvider;

        public SubResourceUriProvider(String path, UriProvider baseProvider) {
            this.path = path;
            this.baseProvider = baseProvider;
        }


        @Override
        public URI getUri() {
            String baseUri = this.baseProvider.getUri().toString();
            if (!baseUri.endsWith("/"))
                baseUri = baseUri + "/";

            try {
                return new URI(baseUri + this.path);
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
