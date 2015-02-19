package com.opower.rest.client.http;

import com.google.common.net.HttpHeaders;
import com.opower.rest.client.BearerAuthCredentials;
import com.opower.rest.client.filters.auth.Oauth2AccessTokenRequester;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code HttpClient} implementation that refreshes the Authorization HTTP header for a request.
 *
 * @author matthew.greenfield
 */
public class AutoRetryAuthorizationRefreshHttpClient implements HttpClient {
    private final HttpClient backend;

    private final ServiceUnavailableRetryStrategy retryStrategy;

    private final Oauth2AccessTokenRequester oauth2AccessTokenRequester;

    private final Log log = LogFactory.getLog(getClass());
    private final int maxRetries;

    /**
     * Creates a new HttpClient that can automatically refresh an authorization header if it is invalid.
     *
     * @param client the backend HttpClient
     * @param oauth2AccessTokenRequester the Oauth2AccessTokenRequester used to refresh the authorization.
     * @param retryStrategy the RetryStrategy to use.
     * @param maxRetries the maximum number of retries.
     */
    public AutoRetryAuthorizationRefreshHttpClient(final HttpClient client, 
                                                   Oauth2AccessTokenRequester oauth2AccessTokenRequester,
                                                   ServiceUnavailableRetryStrategy retryStrategy,
                                                   int maxRetries) {
        super();
        this.backend = checkNotNull(client);
        this.oauth2AccessTokenRequester = checkNotNull(oauth2AccessTokenRequester);
        this.retryStrategy = checkNotNull(retryStrategy);

        checkArgument(maxRetries >= 0, "MaxRetries must be greater than or equal to 0");
        this.maxRetries = maxRetries;
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request)
        throws IOException {
        return execute(target, request, (HttpContext) null);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request,
                         ResponseHandler<? extends T> responseHandler) throws IOException {
        return execute(target, request, responseHandler, null);
    }

    @Override
    public <T> T execute(HttpHost target, HttpRequest request,
                         ResponseHandler<? extends T> responseHandler, HttpContext context)
        throws IOException {
        HttpResponse resp = execute(target, request, context);
        return responseHandler.handleResponse(resp);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request) throws IOException {
        return execute(request, (HttpContext) null);
    }

    @Override
    public HttpResponse execute(HttpUriRequest request, HttpContext context)
        throws IOException {
        URI uri = request.getURI();
        HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort(),
                                         uri.getScheme());
        return execute(httpHost, request, context);
    }

    @Override
    public <T> T execute(HttpUriRequest request,
                         ResponseHandler<? extends T> responseHandler) throws IOException {
        return execute(request, responseHandler, null);
    }

    @Override
    public <T> T execute(HttpUriRequest request,
                         ResponseHandler<? extends T> responseHandler, HttpContext context)
        throws IOException {
        HttpResponse resp = execute(request, context);
        return responseHandler.handleResponse(resp);
    }

    @Override
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        HttpResponse response = this.backend.execute(target, request, context);

        for (int executionCount = 1; executionCount <= this.maxRetries; executionCount++) {
            if (this.retryStrategy.retryRequest(response, executionCount, context)) {
                long nextInterval = this.retryStrategy.getRetryInterval();
                try {
                    this.log.trace("Wait for " + nextInterval);
                    Thread.sleep(nextInterval);

                    BearerAuthCredentials credentials = this.oauth2AccessTokenRequester.refreshAccessToken();

                    request.removeHeaders(HttpHeaders.AUTHORIZATION);
                    request.setHeader(HttpHeaders.AUTHORIZATION, credentials.getHttpHeaderValue());

                    response = this.backend.execute(target, request, context);
                } catch (InterruptedException e) {
                    throw new InterruptedIOException(e.getMessage());
                }
            } else {
                return response;
            }
        }

        return response;
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return this.backend.getConnectionManager();
    }

    @Override
    public HttpParams getParams() {
        return this.backend.getParams();
    }
}
