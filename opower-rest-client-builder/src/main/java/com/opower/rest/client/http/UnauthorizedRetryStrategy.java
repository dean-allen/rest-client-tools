package com.opower.rest.client.http;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of the {@link ServiceUnavailableRetryStrategy} interface.
 * that retries <code>401</code> (Unauthorized Request) responses for a fixed number of times
 * at a fixed interval.
 *
 * @author matthew.greenfield
 */
public class UnauthorizedRetryStrategy implements ServiceUnavailableRetryStrategy {
    private final int maxRetries;

    private final long retryInterval;

    /**
     * Create a new UnauthorizedRetryStrategy.
     *
     * @param maxRetries the maximum number of retries
     * @param retryInterval the time between subsequent retries, in milliseconds
     */
    public UnauthorizedRetryStrategy(int maxRetries, int retryInterval) {
        super();
        checkArgument(maxRetries >= 0, "MaxRetries must be greater than or equal to 0");
        checkArgument(retryInterval >= 0, "Retry interval must be greater than or equal to 0");

        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    @Override
    public boolean retryRequest(final HttpResponse response, int executionCount, final HttpContext context) {
        return executionCount <= this.maxRetries 
               && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
    }

    @Override
    public long getRetryInterval() {
        return this.retryInterval;
    }
}
