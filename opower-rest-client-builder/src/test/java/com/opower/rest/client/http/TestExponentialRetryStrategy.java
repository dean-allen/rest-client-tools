package com.opower.rest.client.http;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicStatusLine;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test for the ExponentialRetryStrategy.
 * @author chris.phillips
 */
@RunWith(EasyMockRunner.class)
public class TestExponentialRetryStrategy extends EasyMockSupport {
    private static final int FOUR = 4;
    private static final int THREE = 3;
    private static final int EIGHT = 8;
    private static final int NINE = 9;
    private static final int TEN = 10;
    private static final int ONE_SECOND = 1000;

    @TestSubject
    private ExponentialRetryStrategy retryStrategy = new ExponentialRetryStrategy();

    @Mock
    private HttpResponse response;

    /**
     * Non 503 responses shouldn't trigger retries.
     */
    @Test
    public void noRetryForNon503() {
        expect(this.response.getStatusLine()).andReturn(makeStatus(Response.Status.NOT_FOUND.getStatusCode()));
        replayAll();
        assertFalse(this.retryStrategy.retryRequest(this.response, 1, null));
        verifyAll();
    }

    /**
     * 503 response triggers retry.
     */
    @Test
    public void doesRetryFor503() {
        expect(this.response.getStatusLine()).andReturn(makeStatus(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()));
        replayAll();
        assertTrue(this.retryStrategy.retryRequest(this.response, 1, null));
        verifyAll();
    }

    /**
     * No retries after max number have been attempted.
     */
    @Test
    public void noRetryAfterMaxRetries() {
        expect(this.response.getStatusLine()).andReturn(makeStatus(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()));
        replayAll();
        assertFalse(this.retryStrategy.retryRequest(this.response, THREE, null));
        verifyAll();
    }

    /**
     * The delay between retries should increase exponentially.
     */
    @Test
    public void delayIsExponential() {
        this.retryStrategy = new ExponentialRetryStrategy(FOUR, ONE_SECOND);
        expect(this.response.getStatusLine()).andReturn(makeStatus(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()));
        replayAll();
        assertTrue(this.retryStrategy.retryRequest(this.response, THREE, null));
        assertThat(this.retryStrategy.getRetryInterval(), is(TimeUnit.SECONDS.toMillis(EIGHT)));
        verifyAll();
    }

    /**
     * The max delay of 10 seconds shouldn't be exceeded.
     */
    @Test
    public void maxDelayIsNeverSurpassed() {
        this.retryStrategy = new ExponentialRetryStrategy(TEN, ONE_SECOND);
        expect(this.response.getStatusLine()).andReturn(makeStatus(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()));
        replayAll();
        assertTrue(this.retryStrategy.retryRequest(this.response, NINE, null));
        assertThat(this.retryStrategy.getRetryInterval(), is(TimeUnit.SECONDS.toMillis(TEN)));
        verifyAll();
    }


    private StatusLine makeStatus(int responseCode) {
        return new BasicStatusLine(new ProtocolVersion("http", 1, 1), responseCode, "");
    }
}
