package com.opower.rest.client.generator.core;

import com.opower.rest.client.generator.specimpl.MultivaluedMapImpl;
import com.opower.rest.client.generator.specimpl.UriBuilderImpl;
import com.opower.rest.client.generator.util.Encode;
import com.opower.rest.client.generator.util.HttpHeaderNames;
import com.opower.rest.client.generator.util.StringConverter;
import com.opower.rest.client.generator.util.StringConverters;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Create a hand coded request to send to the server.  You call methods like accept(), body(), pathParameter()
 * etc. to create the state of the request.  Then you call a get(), post(), etc. method to execute the request.
 * After an execution of a request, the internal state remains the same.  You can invoke the request again.
 * You can clear the request with the clear() method.
 *
 * @author <a href="mailto:sduskis@gmail.com">Solomon Duskis</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */

@SuppressWarnings("unchecked")
public class ClientRequest implements Cloneable {
    private final ProxyConfig proxyConfig;
    protected UriBuilderImpl uri;
    protected ClientExecutor executor;
    protected MultivaluedMap<String, Object> headers;
    protected MultivaluedMap<String, String> queryParameters;
    protected MultivaluedMap<String, String> formParameters;
    protected MultivaluedMap<String, String> pathParameters;
    protected MultivaluedMap<String, String> matrixParameters;
    protected Object body;
    protected Class bodyType;
    protected Type bodyGenericType;
    protected Annotation[] bodyAnnotations;
    protected MediaType bodyContentType;
    protected boolean followRedirects;
    protected String httpMethod;
    protected String finalUri;
    protected List<String> pathParameterList;

    public ClientRequest(String uriTemplate, ClientExecutor executor, ProxyConfig proxyConfig) {
        this((UriBuilderImpl) new UriBuilderImpl().uriTemplate(uriTemplate), executor, proxyConfig);
    }

    public ClientRequest(UriBuilderImpl uriBuilder, ClientExecutor executor, ProxyConfig proxyConfig) {
        this.uri = uriBuilder;
        this.executor = executor;
        this.proxyConfig = proxyConfig;
    }

    public boolean followRedirects() {
        return followRedirects;
    }

    public ClientRequest followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public ClientRequest accept(MediaType accepts) {
        return header(HttpHeaderNames.ACCEPT, accepts.toString());
    }

    public ClientRequest accept(String accept) {
        String curr = (String) getHeadersAsObjects().getFirst(HttpHeaderNames.ACCEPT);
        if (curr != null)
            curr += "," + accept;
        else
            curr = accept;
        getHeadersAsObjects().putSingle(HttpHeaderNames.ACCEPT, curr);
        return this;
    }

    protected String toString(Object object) {
        if (object instanceof String)
            return (String) object;
        StringConverter converter = StringConverters.getStringConverter(object
                .getClass());
        if (converter != null)
            return converter.toString(object);
        else
            return object.toString();
    }

    protected String toHeaderString(Object object) {
        StringConverter converter = StringConverters.getStringConverter(object
                .getClass());
        if (converter != null)
            return converter.toString(object);
        else
            return object.toString();
    }

    public ClientRequest formParameter(String parameterName, Object value) {
        getFormParameters().add(parameterName, toString(value));
        return this;
    }

    public ClientRequest queryParameter(String parameterName, Object value) {
        getQueryParameters().add(parameterName, toString(value));
        return this;
    }

    public ClientRequest matrixParameter(String parameterName, Object value) {
        getMatrixParameters().add(parameterName, toString(value));
        return this;
    }

    public ClientRequest header(String headerName, Object value) {
        getHeadersAsObjects().add(headerName, value);
        return this;
    }

    public ClientRequest cookie(String cookieName, Object value) {
        return cookie(new Cookie(cookieName, toString(value)));
    }

    public ClientRequest cookie(Cookie cookie) {
        return header(HttpHeaders.COOKIE, cookie);
    }

    public ClientRequest pathParameter(String parameterName, Object value) {
        getPathParameters().add(parameterName, toString(value));
        return this;
    }


    public ClientRequest body(MediaType contentType, Object data, Class type,
                              Type genericType, Annotation[] annotations) {
        this.body = data;
        this.bodyContentType = contentType;
        this.bodyGenericType = genericType;
        this.bodyType = type;
        this.bodyAnnotations = annotations;
        return this;
    }

    public Providers getProviders() {
        return this.proxyConfig.getProviders();
    }

    /**
     * @return a copy of all header objects converted to a string
     */
    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, String> rtn = new MultivaluedMapImpl<String, String>();
        if (headers == null) return rtn;
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
            for (Object obj : entry.getValue()) {
                rtn.add(entry.getKey(), toHeaderString(obj));
            }
        }
        return rtn;
    }

    public MultivaluedMap<String, Object> getHeadersAsObjects() {
        if (headers == null)
            headers = new MultivaluedMapImpl<>();
        return headers;
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        if (queryParameters == null)
            queryParameters = new MultivaluedMapImpl<>();
        return queryParameters;
    }

    public MultivaluedMap<String, String> getFormParameters() {
        if (formParameters == null)
            formParameters = new MultivaluedMapImpl<>();
        return formParameters;
    }

    public MultivaluedMap<String, String> getPathParameters() {
        if (pathParameters == null)
            pathParameters = new MultivaluedMapImpl<>();
        return pathParameters;
    }

    public MultivaluedMap<String, String> getMatrixParameters() {
        if (matrixParameters == null)
            matrixParameters = new MultivaluedMapImpl<>();
        return matrixParameters;
    }

    public Object getBody() {
        return body;
    }

    public MediaType getBodyContentType() {
        return bodyContentType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public ClientResponse execute(String httpMethod) throws Exception {
        this.httpMethod = httpMethod;
        this.executor.processFilters(this);
        BaseClientResponse response = (BaseClientResponse) executor.execute(this);
        return response;
    }

    public void writeRequestBody(MultivaluedMap<String, Object> headers,
                                 OutputStream outputStream) throws IOException {
        if (body == null) {
            return;
        }

        MessageBodyWriter writer = this.proxyConfig.getProviders().getMessageBodyWriter(bodyType,
                bodyGenericType, bodyAnnotations, bodyContentType);
        if (writer == null) {
            throw new RuntimeException("could not find writer for content-type "
                    + bodyContentType + " type: " + bodyType.getName());
        }

        writer.writeTo(body, bodyType, bodyGenericType, bodyAnnotations, bodyContentType,
                headers, outputStream);
    }

    /**
     * This method populates all path, matrix, and query parameters and saves it
     * internally. Once its called once it returns the cached value.
     *
     * @return
     * @throws Exception
     */
    public String getUri() throws Exception

    {
        if (finalUri != null)
            return finalUri;

        UriBuilderImpl builder = (UriBuilderImpl) uri.clone();
        if (matrixParameters != null) {
            for (Map.Entry<String, List<String>> entry : matrixParameters
                    .entrySet()) {
                List<String> values = entry.getValue();
                for (String value : values)
                    builder.matrixParam(entry.getKey(), value);
            }
        }
        if (queryParameters != null) {
            for (Map.Entry<String, List<String>> entry : queryParameters
                    .entrySet()) {
                List<String> values = entry.getValue();
                for (String value : values)
                    builder.clientQueryParam(entry.getKey(), value);
            }
        }
        if (pathParameterList != null && !pathParameterList.isEmpty()) {
            finalUri = builder.build(pathParameterList.toArray()).toString();
        } else if (pathParameters != null && !pathParameters.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : pathParameters.entrySet()) {
                List<String> values = entry.getValue();
                for (String value : values) {
                    value = Encode.encodePathAsIs(value);
                    builder.substitutePathParam(entry.getKey(), value, true);
                }
            }
        }
        if (finalUri == null)
            finalUri = builder.build().toString();
        return finalUri;
    }
}
