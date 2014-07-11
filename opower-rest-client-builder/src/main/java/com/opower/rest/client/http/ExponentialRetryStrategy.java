package com.opower.rest.client.http;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * RetryStrategy implementation that increases the retry interval exponentially.
 * @author chris.phillips
 */
public class ExponentialRetryStrategy implements ServiceUnavailableRetryStrategy {

    public static final int DEFAULT_MAX_ATTEMPTS = 2;
    public static final long DEFAULT_RETRY_INTERVAL_IN_MS = 1000;
    private static final long MAX_INTERVAL_IN_MS = 10000;
    
    private final int maxAttempts;
    private final long retryInterval;
    private int executionCount;

    /**
     * Creates an instance with the default settings.
     */
    public ExponentialRetryStrategy() {
        this(DEFAULT_MAX_ATTEMPTS, DEFAULT_RETRY_INTERVAL_IN_MS);
    }

    /**
     * Creates an instance with the specified settings.
     * @param maxAttempts the maximum number of retries 
     * @param retryInterval the base interval between retries
     */
    public ExponentialRetryStrategy(int maxAttempts, long retryInterval) {
        this.maxAttempts = maxAttempts;
        this.retryInterval = retryInterval;
    }

    @Override
    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE) {
            return false;
        }
        this.executionCount = executionCount;
        return this.executionCount < this.maxAttempts;
    }

    @Override
    public long getRetryInterval() {
        return calculateRetryInterval(this.executionCount);
    }

    private long calculateRetryInterval(int executionCount) {
        return Math.round(Math.min(MAX_INTERVAL_IN_MS, Math.pow(2, executionCount) * this.retryInterval));
    }
}
