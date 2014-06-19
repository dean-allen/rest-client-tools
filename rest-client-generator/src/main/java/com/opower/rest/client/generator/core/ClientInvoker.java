package com.opower.rest.client.generator.core;

import com.opower.rest.client.generator.extractors.ClientErrorHandler;
import com.opower.rest.client.generator.extractors.ClientRequestContext;
import com.opower.rest.client.generator.extractors.EntityExtractor;
import com.opower.rest.client.generator.extractors.EntityExtractorFactory;
import com.opower.rest.client.generator.marshallers.ClientMarshallerFactory;
import com.opower.rest.client.generator.marshallers.Marshaller;
import com.opower.rest.client.generator.specimpl.UriBuilderImpl;
import com.opower.rest.client.generator.util.MediaTypeHelper;

import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("unchecked")
public class ClientInvoker implements MethodInvoker {
    protected String httpMethod;
    protected Method method;
    protected Class declaring;
    protected MediaType accepts;
    protected Marshaller[] marshallers;
    protected ClientExecutor executor;
    protected boolean followRedirects;
    protected EntityExtractor extractor;
    protected EntityExtractorFactory extractorFactory;
    protected UriProvider baseUriProvider;
    private final ProxyConfig proxyConfig;


    public ClientInvoker(UriProvider baseUriProvider, Class declaring, Method method, ProxyConfig config) {
        this.proxyConfig = config;
        this.declaring = declaring;
        this.method = method;
        this.marshallers = ClientMarshallerFactory.createMarshallers(declaring, method);
        this.executor = config.getExecutor();
        accepts = MediaTypeHelper.getProduces(declaring, method);
        this.baseUriProvider = checkNotNull(baseUriProvider);
        this.extractorFactory = config.getExtractorFactory();
        this.extractor = extractorFactory.createExtractor(method);
    }

    public Method getMethod() {
        return method;
    }

    public Object invoke(Object[] args) {

        ClientRequest request = createRequest(args);

        BaseClientResponse clientResponse = null;
        try {
            clientResponse = (BaseClientResponse) request.execute(httpMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ClientErrorHandler errorHandler = new ClientErrorHandler(this.proxyConfig.getClientErrorInterceptors());
        clientResponse.setAttributeExceptionsTo(method.toString());
        clientResponse.setAnnotations(method.getAnnotations());
        ClientRequestContext clientRequestContext = new ClientRequestContext(request, clientResponse, errorHandler);
        return extractor.extractEntity(clientRequestContext);
    }

    protected ClientRequest createRequest(Object[] args) {
        UriBuilderImpl uri = new UriBuilderImpl();
        uri.uri(baseUriProvider.getUri());
        if (declaring.isAnnotationPresent(Path.class)) uri.path(declaring);
        if (method.isAnnotationPresent(Path.class)) uri.path(method);
        ClientRequest request = new ClientRequest(uri, executor, proxyConfig);
        if (accepts != null) request.header(HttpHeaders.ACCEPT, accepts.toString());

        boolean isClientResponseResult = ClientResponse.class.isAssignableFrom(method.getReturnType());
        request.followRedirects(!isClientResponseResult || this.followRedirects);

        for (int i = 0; i < marshallers.length; i++) {
            marshallers[i].build(request, args[i]);
        }
        return request;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

}
