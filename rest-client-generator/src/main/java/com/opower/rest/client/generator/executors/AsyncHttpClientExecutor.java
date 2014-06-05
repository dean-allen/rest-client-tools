package com.opower.rest.client.generator.executors;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.opower.rest.client.generator.core.BaseClientResponse;
import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import com.opower.rest.client.generator.core.ClientResponse;
import com.opower.rest.client.generator.core.SelfExpandingBufferredInputStream;
import com.opower.rest.client.generator.util.CaseInsensitiveMap;

import javax.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @author chris.phillips
 */
public class AsyncHttpClientExecutor extends AbstractClientExecutor {

    private final AsyncHttpClient httpClient;

    public AsyncHttpClientExecutor() {
        this(new AsyncHttpClientConfig.Builder().build(), ImmutableList.<ClientRequestFilter>of());
    }

    public AsyncHttpClientExecutor(List<ClientRequestFilter> requestFilters) {
        this(new AsyncHttpClientConfig.Builder().build(), requestFilters);
    }

    public AsyncHttpClientExecutor(AsyncHttpClientConfig clientConfig) {
        this(clientConfig, ImmutableList.<ClientRequestFilter>of());
    }

    public AsyncHttpClientExecutor(AsyncHttpClientConfig clientConfig, List<ClientRequestFilter> requestFilters) {
        super(requestFilters);
        this.httpClient = new AsyncHttpClient(clientConfig);
    }

    @Override
    public ClientResponse execute(ClientRequest request) throws Exception {
        RequestBuilder requestBuilder = new RequestBuilder(request.getHttpMethod()).setUrl(request.getUri());
        requestBuilder.setFollowRedirects(request.followRedirects());
        if(request.getBody() != null && !request.getFormParameters().isEmpty()) {
            throw new RuntimeException("You cannot send both form parameters and an entity body");
        }

        commitHeaders(request, requestBuilder);

        if (!request.getFormParameters().isEmpty()) {
            for (Map.Entry<String, List<String>> formParam : request.getFormParameters().entrySet()) {
                List<String> values = formParam.getValue();
                for (String value : values) {
                    requestBuilder.addParameter(formParam.getKey(), value);
                }
            }
        } else if (request.getBody() != null) {
            if (request.getHttpMethod().equals("GET")) {
                throw new RuntimeException("A GET request cannot have a body.");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            request.writeRequestBody(request.getHeadersAsObjects(), baos);
            requestBuilder.setBody(baos.toByteArray());
        }


        Response rawResponse = this.httpClient.executeRequest(requestBuilder.build()).get();

        BaseClientResponse response = new BaseClientResponse(new SimpleBaseClientResponseStreamFactory(rawResponse), this);

        response.setStatus(rawResponse.getStatusCode());
        response.setHeaders(extractHeaders(rawResponse));
        response.setProviders(request.getProviders());
        return response;

    }

    public void commitHeaders(ClientRequest request, RequestBuilder requestBuilder) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            List<String> values = header.getValue();
            for (String value : values) {
                requestBuilder.addHeader(header.getKey(), value);
            }
        }
    }

    /**
     * Extracts the headers from the given HttpResponse.
     * @param response the HttpResponse to get the headers from
     * @return A map of the headers found on the HttpResponse
     */
    public static CaseInsensitiveMap<String> extractHeaders(
            Response response) {
        CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<String>();
        FluentCaseInsensitiveStringsMap headerMap = response.getHeaders();
        for (String headerName : headerMap.keySet()) {
            for(String headerValue : headerMap.get(headerName)) {
                headers.add(headerName, headerValue);
            }
        }
        return headers;
    }

    @Override
    public void close() throws Exception {

    }

    private class SimpleBaseClientResponseStreamFactory implements BaseClientResponse.BaseClientResponseStreamFactory {
        private final Response res;
        private InputStream stream;

        private SimpleBaseClientResponseStreamFactory(Response res) {
            this.res = res;
        }

        public InputStream getInputStream() throws IOException {
            if (stream == null) {
                InputStream rawStream = this.res.getResponseBodyAsStream();
                if(rawStream == null) {
                    return null;
                }
                this.stream =  new SelfExpandingBufferredInputStream(rawStream);
            }
            return this.stream;
        }
        
        public void performReleaseConnection() {
            try {
                Closeables.close(this.stream, true);
            } catch (IOException ex) {
                Throwables.propagate(ex);
            }
        }
    }
}
