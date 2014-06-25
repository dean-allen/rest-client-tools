package com.opower.rest.client.generator.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * Convenience enum so we don't have to be too verbose in tests.
 * @author chris.phillips
 */
public enum TestHystrixGroupKeys implements HystrixCommandGroupKey {
    TEST_GROUP
}
