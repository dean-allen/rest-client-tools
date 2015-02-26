package com.opower.rest.client.http;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link com.opower.rest.client.http.UnauthorizedRetryStrategy}.
 *
 * @author matthew.greenfield
 */
public class TestUnauthorizedRetryStrategy {
    private static final UnauthorizedRetryStrategy SINGLE_RETRY_STRATEGY = new UnauthorizedRetryStrategy(1, 1);

    private HttpResponse response;
    private StatusLine statusLine;

    /**
     * Test setup.
     */
    @Before
    public void setUp() {
        this.response = createMock(HttpResponse.class);
        this.statusLine = createMock(StatusLine.class);
        expect(this.response.getStatusLine()).andReturn(this.statusLine).once();
    }

    /**
     * Test that a UnauthorizedRetryStrategy cannot be created with a negative retry attempts.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRetryStrategyWithNegativeTries() {
        new UnauthorizedRetryStrategy(-1, 0);
    }

    /**
     * Test that a UnauthorizedRetryStrategy cannot be created with a negative retry interval.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRetryStrategyWithNegativeInterval() {
        new UnauthorizedRetryStrategy(0, -1);
    }

    /**
     * Test that a retry is made if the request response is 401 (unauthorized).
     */
    @Test
    public void testRetryRequest() {
        expect(this.statusLine.getStatusCode()).andReturn(SC_UNAUTHORIZED);

        replay(this.response, this.statusLine);
        assertTrue(SINGLE_RETRY_STRATEGY.retryRequest(this.response, 1, createNiceMock(HttpContext.class)));
        verify(this.response, this.statusLine);
    }

    /**
     * Test that if the number of attempts have been exhausted, a retry is not issued.
     */
    @Test
    public void testRetryRequestsExhausted() {
        assertFalse(SINGLE_RETRY_STRATEGY.retryRequest(this.response, 2, createNiceMock(HttpContext.class)));
    }

    /**
     * Test that the the response was a 200 and that the request should not be retried.
     */
    @Test
    public void testShouldNotRetryRequest() {
        expect(this.statusLine.getStatusCode()).andReturn(SC_OK);

        replay(this.response, this.statusLine);
        assertFalse(SINGLE_RETRY_STRATEGY.retryRequest(this.response, 1, createNiceMock(HttpContext.class)));
        verify(this.response, this.statusLine);
    }
}
