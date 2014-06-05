package com.opower.rest.client.generator.executors;

import com.google.common.collect.ImmutableList;
import com.opower.rest.client.generator.core.BaseClientResponse;
import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import com.opower.rest.client.generator.core.ClientResponse;
import com.opower.rest.client.generator.core.SelfExpandingBufferredInputStream;
import com.opower.rest.client.generator.util.CaseInsensitiveMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * ClientExecutor implementation that uses HttpClient 4.
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApacheHttpClient4Executor extends AbstractClientExecutor {
    protected final HttpClient httpClient;
    protected boolean createdHttpClient;
    protected HttpContext httpContext;
    protected boolean closed;

    /**
     * Create an instance using the DefaultHttpClient.
     */
    public ApacheHttpClient4Executor() {
       this(new DefaultHttpClient(new PoolingClientConnectionManager()), ImmutableList.<ClientRequestFilter>of());
    }

    public ApacheHttpClient4Executor(List<ClientRequestFilter> requestFilters ) {
        this(new DefaultHttpClient(new PoolingClientConnectionManager()), requestFilters);
    }

    /**
     * Create an instance using the specified HttpClient.
     * @param httpClient the HttpClient to use
     */
    public ApacheHttpClient4Executor(HttpClient httpClient) {
        this(httpClient, ImmutableList.<ClientRequestFilter>of());
    }

    public ApacheHttpClient4Executor(HttpClient httpClient, List<ClientRequestFilter> requestFilters) {
        super(requestFilters);
        this.httpClient = checkNotNull(httpClient);
    }

    /**
     * Extracts the headers from the given HttpResponse.
     * @param response the HttpResponse to get the headers from
     * @return A map of the headers found on the HttpResponse
     */
    public static CaseInsensitiveMap<String> extractHeaders(
            HttpResponse response) {
        final CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<String>();

        for (Header header : response.getAllHeaders()) {
            headers.add(header.getName(), header.getValue());
        }
        return headers;
    }


    @Override
    @SuppressWarnings("unchecked")
    public ClientResponse execute(ClientRequest request) throws Exception {
        String uri = request.getUri();
        final HttpRequestBase httpMethod = createHttpMethod(uri, request.getHttpMethod());
        loadHttpMethod(request, httpMethod);

        final HttpResponse res = this.httpClient.execute(httpMethod, this.httpContext);

        BaseClientResponse response = new BaseClientResponse(new SimpleBaseClientResponseStreamFactory(res), this);

        response.setStatus(res.getStatusLine().getStatusCode());
        response.setHeaders(extractHeaders(res));
        response.setProviders(request.getProviders());
        return response;
    }

    private HttpRequestBase createHttpMethod(String url, String restVerb) {
        if ("GET".equals(restVerb)) {
            return new HttpGet(url);
        } else if ("POST".equals(restVerb)) {
            return new HttpPost(url);
        } else {
            final String verb = restVerb;
            return new HttpPost(url) {
                @Override
                public String getMethod() {
                    return verb;
                }
            };
        }
    }

    public void loadHttpMethod(final ClientRequest request, HttpRequestBase httpMethod) throws Exception {
        if (httpMethod instanceof HttpGet && request.followRedirects()) {
            HttpClientParams.setRedirecting(httpMethod.getParams(), true);
        } else {
            HttpClientParams.setRedirecting(httpMethod.getParams(), false);
        }

        if (request.getBody() != null && !request.getFormParameters().isEmpty())
            throw new RuntimeException("You cannot send both form parameters and an entity body");

        if (!request.getFormParameters().isEmpty()) {
            commitHeaders(request, httpMethod);
            HttpPost post = (HttpPost) httpMethod;

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();

            for (Map.Entry<String, List<String>> formParam : request.getFormParameters().entrySet()) {
                List<String> values = formParam.getValue();
                for (String value : values) {
                    formparams.add(new BasicNameValuePair(formParam.getKey(), value));
                }
            }

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(entity);
        } else if (request.getBody() != null) {
            if (httpMethod instanceof HttpGet) throw new RuntimeException("A GET request cannot have a body.");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                request.writeRequestBody(request.getHeadersAsObjects(), baos);
                ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray()) {
                    @Override
                    public Header getContentType() {
                        return new BasicHeader("Content-Type", request.getBodyContentType().toString());
                    }
                };
                HttpPost post = (HttpPost) httpMethod;
                commitHeaders(request, httpMethod);
                post.setEntity(entity);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else // no body
        {
            commitHeaders(request, httpMethod);
        }
    }

    public void commitHeaders(ClientRequest request, HttpRequestBase httpMethod) {
        MultivaluedMap<String, String> headers = request.getHeaders();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            List<String> values = header.getValue();
            for (String value : values) {
//               System.out.println(String.format("setting %s = %s", header.getKey(), value));
                httpMethod.addHeader(header.getKey(), value);
            }
        }
    }

    @Override
    public void close() {
        if (closed)
            return;

        if (createdHttpClient && httpClient != null) {
            ClientConnectionManager manager = httpClient.getConnectionManager();
            if (manager != null) {
                manager.shutdown();
            }
        }
        closed = true;
    }

    //CHECKSTYLE:OFF
    @Override
    public void finalize() throws Throwable {
        close();
        super.finalize();
    }//CHECKSTYLE:ON


    private class SimpleBaseClientResponseStreamFactory implements BaseClientResponse.BaseClientResponseStreamFactory {
        private final HttpResponse res;
        private InputStream stream;

        private SimpleBaseClientResponseStreamFactory(HttpResponse res) {
            this.res = res;
        }


        public InputStream getInputStream() throws IOException {
            if (this.stream == null) {
                HttpEntity entity = this.res.getEntity();
                if (entity == null) { return null; }
                this.stream = new SelfExpandingBufferredInputStream(entity.getContent());
            }
            return this.stream;
        }
        public void performReleaseConnection() {
            // Apache Client 4 is stupid,  You have to get the InputStream and close it if there is an entity
            // otherwise the connection is never released.  There is, of course, no close() method on response
            // to make this easier.
            try {
                if (this.stream != null) {
                    this.stream.close();
                } else {
                    InputStream is = getInputStream();
                    if (is != null) {  is.close(); }
                }
            } catch (Exception ignore) { }
        }
    }

}
