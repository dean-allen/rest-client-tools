package com.opower.rest.client.http;

import com.opower.auth.oauth2.BearerToken;
import com.opower.rest.client.BearerAuthCredentials;
import com.opower.rest.client.filters.auth.Oauth2AccessTokenRequester;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;

/**
 * Unit tests for {@link com.opower.rest.client.http.AutoRetryAuthorizationRefreshHttpClient}.
 *
 * @author matthew.greenfield
 */
public class TestAutoRetryAuthorizationRefreshHttpClient extends EasyMockSupport {
    private static final HttpHost HOST = new HttpHost("localhost");
    private final HttpRequest request = createNiceMock(HttpRequest.class);
    private final HttpContext context = createNiceMock(HttpContext.class);
    private HttpClient httpClient;
    private HttpClient client;
    private Oauth2AccessTokenRequester oauth2AccessTokenRequester;
    private ServiceUnavailableRetryStrategy retryStrategy;

    /**
     * Test setup.
     */
    @Before
    public void setUp() {
        this.client = createMock(HttpClient.class);
        this.oauth2AccessTokenRequester = createMock(Oauth2AccessTokenRequester.class);
        this.retryStrategy = createMock(ServiceUnavailableRetryStrategy.class);

        this.httpClient = new AutoRetryAuthorizationRefreshHttpClient(this.client,
                                                                      this.oauth2AccessTokenRequester,
                                                                      this.retryStrategy,
                                                                      1);
    }

    /**
     * Test that if the request needs to be retried, the token refresh is called first before retrying.
     *
     * @throws IOException if something bad happens.
     */
    @Test
    public void testRetryCallsRefreshToken() throws IOException {
        expect(this.client.execute(anyObject(HttpHost.class), anyObject(HttpRequest.class), anyObject(HttpContext.class)))
                .andReturn(createNiceMock(HttpResponse.class)).times(2);

        expect(this.oauth2AccessTokenRequester.refreshAccessToken())
                .andReturn(new BearerAuthCredentials(new BearerToken("blah"))).once();

        expect(this.retryStrategy.retryRequest(anyObject(HttpResponse.class), anyInt(), anyObject(HttpContext.class)))
                .andReturn(true).once();
        expect(this.retryStrategy.getRetryInterval()).andReturn(0L);

        replayAll();

        this.httpClient.execute(HOST, this.request, this.context);

        verifyAll();
    }

    /**
     * Test that if the request needs to be retried, the token refresh is called first before retrying.
     *
     * @throws IOException if something bad happens.
     */
    @Test
    public void testNoRetryNeeded() throws IOException {
        expect(this.client.execute(anyObject(HttpHost.class), anyObject(HttpRequest.class), anyObject(HttpContext.class)))
                .andReturn(createNiceMock(HttpResponse.class)).once();

        expect(this.retryStrategy.retryRequest(anyObject(HttpResponse.class), anyInt(), anyObject(HttpContext.class)))
                .andReturn(false).once();

        replayAll();

        this.httpClient.execute(HOST, this.request, this.context);

        verifyAll();
    }
}
