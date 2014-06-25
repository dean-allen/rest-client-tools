package com.opower.rest.client.generator.hystrix;

import com.netflix.hystrix.HystrixCommandKey;

/**
 * Convenience Enum so we don't have to be too verbose in tests.
 * @author chris.phillips
 */
public enum TestHystrixCommandKeys implements HystrixCommandKey {
    TEST_CMD
}
