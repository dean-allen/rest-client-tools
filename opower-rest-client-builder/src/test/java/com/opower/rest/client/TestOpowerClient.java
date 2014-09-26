package com.opower.rest.client;

import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.opower.rest.client.generator.core.SimpleUriProvider;
import com.opower.rest.client.generator.core.UriProvider;
import com.opower.rest.test.resource.FrobResource;
import java.lang.reflect.Method;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for the OpowerClient.
 * @author chris.phillips
 */
public class TestOpowerClient {

    private static final int THIRTY = 30;
    private static final int ELEVEN = 11;
    private static final String URL = "http://localhost";
    private static final String SERVICE_NAME = "test-v1";
    private static final String CLIENT_ID = "test-client-id";
    private static final UriProvider URI_PROVIDER = new SimpleUriProvider(URL);
    /**
     * By default each hystrixcommand gets its own thread pool with a core size of 10. The connection pool for the httpclient
     * shoudl have 10 * the number of methods on the resource interface by default; That ensures it can handle the max number of
     * concurrent commands as hystrix will allow.
     */
    @Test
    public void defaultHystrixThreadPoolSizeDictatesHttpClientPoolConfiguration() {
        OpowerClient opowerClient = new OpowerClient.Builder(FrobResource.class,
                                                             URI_PROVIDER, SERVICE_NAME, CLIENT_ID);
        HttpClient client = opowerClient.prepareHttpClient();
        PoolingClientConnectionManager connectionManager = (PoolingClientConnectionManager)client.getConnectionManager();
        assertThat(connectionManager.getMaxTotal(), is(THIRTY));
        assertThat(connectionManager.getDefaultMaxPerRoute(), is(THIRTY));
    }

    /**
     * Test that ensures when a thread pool size for the hystrix command has been changed, it is reflected in the size
     * of the pool for the httpclient.
     * @throws java.lang.Exception for convenience
     */
    @Test
    public void adjustedHystrixThreadPoolSizeDictatesHttpClientPoolConfiguration() throws Exception {
        OpowerClient opowerClient = new OpowerClient.Builder(FrobResource.class, URI_PROVIDER,
                                                             SERVICE_NAME, CLIENT_ID);
        Method findFrob = FrobResource.class.getMethod("findFrob", String.class);
        opowerClient.methodThreadPoolProperties(findFrob, new ConfigurationCallback<HystrixThreadPoolProperties.Setter>() {
            @Override
            public void configure(HystrixThreadPoolProperties.Setter setter) {
                setter.withCoreSize(ELEVEN);
            }
        });
        HttpClient client = opowerClient.prepareHttpClient();
        PoolingClientConnectionManager connectionManager = (PoolingClientConnectionManager)client.getConnectionManager();
        assertThat(connectionManager.getMaxTotal(), is(THIRTY + 1));
        assertThat(connectionManager.getDefaultMaxPerRoute(), is(THIRTY + 1));
    }

}
