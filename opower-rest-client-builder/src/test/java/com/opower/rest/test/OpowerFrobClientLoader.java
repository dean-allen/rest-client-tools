package com.opower.rest.test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixCommandProperties;
import com.opower.rest.client.ConfigurationCallback;
import com.opower.rest.client.OpowerClient;
import com.opower.rest.client.generator.core.SimpleUriProvider;
import com.opower.rest.client.generator.core.UriProvider;
import com.opower.rest.test.jetty.JettyServerBuilder;
import com.opower.rest.test.resource.FrobClientLoader;
import com.opower.rest.test.resource.FrobResource;
import java.util.Map;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.eclipse.jetty.server.Server;

/**
 * FrobClientLoader for Opower specific client instances. Creates client that both enable and
 * disable authentication, also clients that do and do not use curator.
 * @author chris.phillips
 */
public class OpowerFrobClientLoader implements FrobClientLoader {

    private static final TestingServer TEST_SERVER = initServer();
    private static final ConfigurationCallback<HystrixCommandProperties.Setter> TIMEOUT_CALLBACK =
            new ConfigurationCallback<HystrixCommandProperties.Setter>() {
                @Override
                public void configure(HystrixCommandProperties.Setter setter) {
                    setter.withExecutionIsolationThreadTimeoutInMilliseconds(TIMEOUT);
                }
            };
    
    private static final int AUTH_PORT = 7071;
    private static final int RETRY_WAIT = 1000;
    private static final int TIMEOUT = 10000;
    private static final String SERVICE_NAME = "frob-v1";
    private static final String CLIENT_ID = "testClientId";
    private static final String CLIENT_SECRET = "testClientSecret";
    private static final UriProvider FROB_URI_PROVIDER = new SimpleUriProvider("http://localhost:7000/");
    private static final UriProvider AUTH_URI_PROVIDER = new SimpleUriProvider("http://localhost:7071/");
    private CuratorFramework curatorClient;
    private ServiceDiscovery serviceDiscovery;
    private Server authServer;


    @Override
    public Map<String, FrobResource> clientsToTest(int port, String type) {
        try {
            this.curatorClient = CuratorFrameworkFactory.builder()
                    .retryPolicy(new RetryOneTime(RETRY_WAIT))
                    .connectString(TEST_SERVER.getConnectString()).build();
            this.curatorClient.start();

            ServiceDiscoveryBuilder<Void> discoveryBuilder = ServiceDiscoveryBuilder.builder(Void.class);
            discoveryBuilder.client(this.curatorClient);
            discoveryBuilder.basePath("/services");

            this.serviceDiscovery = discoveryBuilder.build();
            this.serviceDiscovery.start();

            registerService(SERVICE_NAME, port);
            registerService(OpowerClient.Builder.AUTH_SERVICE_NAME, AUTH_PORT);

            this.authServer = JettyServerBuilder.initServer(AUTH_PORT, OpowerFrobClientLoader.class.getResource(
                    String.format("/auth-server/%s/web.xml", type)).toString());
            this.authServer.start();

        } catch (Exception e) {
            Throwables.propagate(e);
        }
        try {
            return new ImmutableMap.Builder<String, FrobResource>()
                               .put("noAuth", noAuth())
                               .put("withAuthSeparateUriProviders", withAuthSeparateUriProviders())
                               .put("withAuthSameUriProviders", withAuthSameUriProvider())
                               .put("deprecatedwithZK", deprecatedDefaultClient())
                               .put("deprecatedwithZKAndAuth", deprecatedDefaultWithAuth())
                               .put("deprecatedNoZKNoAuth", deprecatedNoZKNoAuth())
                               .put("deprecatedNoZKWithAuth", deprecatednoZKWithAuth())
                               .build();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private void registerService(String serviceName, int port) throws Exception {
        ServiceInstanceBuilder<Void> instanceBuilder = ServiceInstance.builder();
        instanceBuilder.address("localhost");
        instanceBuilder.port(port);
        instanceBuilder.name(serviceName);
        this.serviceDiscovery.registerService(instanceBuilder.build());
    }

    private FrobResource noAuth() throws Exception {
        return new OpowerClient.Builder<>(AnnotatedFrobResource.class,FROB_URI_PROVIDER, CLIENT_ID)
                .commandProperties(TIMEOUT_CALLBACK)
                .disableMetrics()
                .build();
    }

    private FrobResource withAuthSeparateUriProviders() throws Exception {
        return new OpowerClient.Builder<>(AnnotatedFrobResource.class, FROB_URI_PROVIDER, CLIENT_ID)
                .disableMetrics()
                .clientSecret(CLIENT_SECRET, AUTH_URI_PROVIDER)
                .commandProperties(TIMEOUT_CALLBACK)
                .build();
    }

    private FrobResource withAuthSameUriProvider() throws Exception {
        return new OpowerClient.Builder<>(AnnotatedFrobResource.class, AUTH_URI_PROVIDER, CLIENT_ID)
                .disableMetrics()
                .clientSecret(CLIENT_SECRET)
                .commandProperties(TIMEOUT_CALLBACK)
                .build();
    }

    private FrobResource deprecatedNoZKNoAuth() throws Exception {
        return new OpowerClient.Builder<>(FrobResource.class, FROB_URI_PROVIDER, SERVICE_NAME, CLIENT_ID)
                .commandProperties(TIMEOUT_CALLBACK)
                .disableMetrics()
                .build();
    }

    private FrobResource deprecatednoZKWithAuth() throws Exception {
        return new OpowerClient.Builder<>(FrobResource.class, FROB_URI_PROVIDER, SERVICE_NAME, CLIENT_ID)
                .disableMetrics()
                .clientSecret(CLIENT_SECRET, AUTH_URI_PROVIDER)
                .commandProperties(TIMEOUT_CALLBACK)
                .build();
    }

    private FrobResource deprecatedDefaultClient() throws Exception {
        return new OpowerClient.Builder<>(FrobResource.class, this.serviceDiscovery, SERVICE_NAME, CLIENT_ID)
                .commandProperties(TIMEOUT_CALLBACK)
                .disableMetrics()
                .build();
    }

    private FrobResource deprecatedDefaultWithAuth() throws Exception {
        return new OpowerClient.Builder<>(FrobResource.class, this.serviceDiscovery, SERVICE_NAME, CLIENT_ID)
                .commandProperties(TIMEOUT_CALLBACK)
                .disableMetrics()
                .clientSecret(CLIENT_SECRET)
                .build();
    }

    private static TestingServer initServer() {
        try {
            return new TestingServer();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
