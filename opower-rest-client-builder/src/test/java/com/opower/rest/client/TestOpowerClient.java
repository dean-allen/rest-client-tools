package com.opower.rest.client;

import com.google.common.collect.Iterables;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.opower.rest.client.generator.core.SimpleUriProvider;
import com.opower.rest.client.generator.core.UriProvider;
import com.opower.rest.test.resource.FrobResource;
import java.lang.reflect.Method;
import java.util.Collection;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for the OpowerClient.
 * @author chris.phillips
 */
public class TestOpowerClient {

    private static final int TEN = 10;
    private static final int TWENTY_SECONDS = 20000;
    private static final int TWO_SECONDS = 2000;
    private static final int SEVEN_SECONDS = 7000;
    private static final int NUM_METHODS = FrobResource.class.getDeclaredMethods().length;
    private static final int EXPECTED_POOL_SIZE = NUM_METHODS * TEN;
    private static final int ELEVEN = 11;
    private static final String URL = "http://localhost";
    private static final String SERVICE_NAME = "test-v1";
    private static final String CLIENT_ID = "test-client-id";
    private static final UriProvider URI_PROVIDER = new SimpleUriProvider(URL);

    /**
     * Initializes the system property to ensure the RuntimeDelegate gets properly loaded.
     */
    @BeforeClass
    public static void init() {
        System.setProperty("javax.ws.rs.ext.RuntimeDelegate","com.opower.rest.client.generator.core.BasicRuntimeDelegate");
    }

    /**
     * By default each hystrixcommand gets its own thread pool with a core size of 10. The connection pool for the httpclient
     * shoudl have 10 * the number of methods on the resource interface by default; That ensures it can handle the max number of
     * concurrent commands as hystrix will allow.
     */
    @Test
    public void defaultHystrixThreadPoolSizeDictatesHttpClientPoolConfiguration() {
        OpowerClient opowerClient = new OpowerClient.Builder<>(FrobResource.class, URI_PROVIDER, SERVICE_NAME, CLIENT_ID);
        HttpClient client = opowerClient.prepareHttpClient();
        PoolingClientConnectionManager connectionManager = (PoolingClientConnectionManager)client.getConnectionManager();
        assertThat(connectionManager.getMaxTotal(), is(EXPECTED_POOL_SIZE));
        assertThat(connectionManager.getDefaultMaxPerRoute(), is(EXPECTED_POOL_SIZE));
    }

    /**
     * Test that ensures when a thread pool size for the hystrix command has been changed, it is reflected in the size
     * of the pool for the httpclient.
     * @throws java.lang.Exception for convenience
     */
    @Test
    public void adjustedHystrixThreadPoolSizeDictatesHttpClientPoolConfiguration() throws Exception {
        OpowerClient opowerClient = new OpowerClient.Builder<>(FrobResource.class, URI_PROVIDER, SERVICE_NAME, CLIENT_ID);
        Method findFrob = FrobResource.class.getMethod("findFrob", String.class);
        opowerClient.methodThreadPoolProperties(findFrob, new ConfigurationCallback<HystrixThreadPoolProperties.Setter>() {
            @Override
            public void configure(HystrixThreadPoolProperties.Setter setter) {
                setter.withCoreSize(ELEVEN);
            }
        });
        HttpClient client = opowerClient.prepareHttpClient();
        PoolingClientConnectionManager connectionManager = (PoolingClientConnectionManager)client.getConnectionManager();
        assertThat(connectionManager.getMaxTotal(), is(EXPECTED_POOL_SIZE + 1));
        assertThat(connectionManager.getDefaultMaxPerRoute(), is(EXPECTED_POOL_SIZE + 1));
    }

    /**
     * Ensures that the default timeout for hystrix commands is correctly set.
     * @throws Exception for convenience
     */
    @Test
    public void defaultHystrixTimeout() throws Exception {
        OpowerClient opowerClient = new OpowerClient.Builder<>(FrobResource.class, URI_PROVIDER, SERVICE_NAME, CLIENT_ID);
        opowerClient.hystrixCommandTimeout();
        Collection<HystrixCommandProperties.Setter> setters = opowerClient.getCommandPropertiesMap().values();
        HystrixCommandProperties.Setter setter = Iterables.getFirst(setters, null);
        assertThat(setter.getExecutionIsolationThreadTimeoutInMilliseconds(), is(SEVEN_SECONDS));
    }

    /**
     * Ensures that if the hystrix timeout is set lower than the duration required by the configured retries, then
     * it will be adjusted to accommodate the retries.
     * @throws Exception for convenience
     */
    @Test
    public void hystrixTimeoutSetTooLow() throws Exception {
        OpowerClient opowerClient = new OpowerClient.Builder<>(FrobResource.class, URI_PROVIDER, SERVICE_NAME, CLIENT_ID);
        opowerClient.commandProperties(new ConfigurationCallback<HystrixCommandProperties.Setter>() {
            @Override
            public void configure(HystrixCommandProperties.Setter setter) {
                setter.withExecutionIsolationThreadTimeoutInMilliseconds(TWO_SECONDS);
            }
        });
        opowerClient.hystrixCommandTimeout();
        Collection<HystrixCommandProperties.Setter> setters = opowerClient.getCommandPropertiesMap().values();
        HystrixCommandProperties.Setter setter = Iterables.getFirst(setters, null);
        assertThat(setter.getExecutionIsolationThreadTimeoutInMilliseconds(), is(SEVEN_SECONDS));
    }

    /**
     * Ensures that hystrix timeouts set higher than that required by the retry settings are applied correctly.
     * @throws Exception for convenience
     */
    @Test
    public void longerHystrixTimeoutsAreAccepted() throws Exception {
        OpowerClient opowerClient = new OpowerClient.Builder<>(FrobResource.class, URI_PROVIDER, SERVICE_NAME, CLIENT_ID);
        opowerClient.commandProperties(new ConfigurationCallback<HystrixCommandProperties.Setter>() {
            @Override
            public void configure(HystrixCommandProperties.Setter setter) {
                setter.withExecutionIsolationThreadTimeoutInMilliseconds(TWENTY_SECONDS);
            }
        });
        opowerClient.hystrixCommandTimeout();
        Collection<HystrixCommandProperties.Setter> setters = opowerClient.getCommandPropertiesMap().values();
        HystrixCommandProperties.Setter setter = Iterables.getFirst(setters, null);
        assertThat(setter.getExecutionIsolationThreadTimeoutInMilliseconds(), is(TWENTY_SECONDS));
    }

}
