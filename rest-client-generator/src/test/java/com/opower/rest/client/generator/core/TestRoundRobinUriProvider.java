package com.opower.rest.client.generator.core;

import java.util.Random;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for the RoundRobinUriProvider.
 * @author chris.phillips
 */
public class TestRoundRobinUriProvider {

    private static final String TEST_URI_1 = "http://localhost:8080";
    private static final String TEST_URI_2 = "http://localhost:8081";
    private static final int MAX_ITERATIONS = 1000;
    /**
     * Makes sure one URI will be returned over and over.
     */
    @Test
    public void oneURIWorks() {
        UriProvider uriProvider = new RoundRobinUriProvider(TEST_URI_1);

        Random r = new Random();

        for (int i = 0; i < r.nextInt(MAX_ITERATIONS); i++) {
            assertThat(uriProvider.getUri().toString(), is(TEST_URI_1));
        }
    }

    /**
     * Makes sure that the URIs are returned over and over in round robin fashion.
     */
    @Test
    public void multipleUrisWork() {
        UriProvider uriProvider = new RoundRobinUriProvider(TEST_URI_1, TEST_URI_2);
        Random r = new Random();
        for (int i = 1; i < r.nextInt(MAX_ITERATIONS); i++) {
            assertThat(uriProvider.getUri().toString(), is(i % 2 == 0 ? TEST_URI_2 :TEST_URI_1));
        }

    }
}
