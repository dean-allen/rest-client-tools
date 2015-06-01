package com.opower.rest.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.opower.rest.client.ConfigurationCallback;
import com.opower.rest.client.OpowerClient;
import com.opower.rest.client.generator.core.SimpleUriProvider;
import com.opower.rest.client.model.TestModelMixin;
import com.opower.rest.client.model.TestModelWithFields;
import com.opower.rest.test.jetty.JettyRule;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * End to end tests for the OpowerClient.
 *
 * @author matthew.greenfield
 */
public class IntTestOpowerClient {
    public static final int PORT = 7000;
    @ClassRule
    public static final JettyRule JETTY_RULE = 
            new JettyRule(PORT, AuthApplication.class.getResource("/auth-server/jersey/1/web.xml").toString());
    private static final String CLIENT_ID = "client.id";
    private static final String RESOURCE_URI = "http://localhost:7000/";
    private ExampleResource exampleResource;

    /**
     * Test that if a request to an endpoint fails with a 401 status code, the token is refreshed and another attempt is made.
     */
    @Test
    public void testAccessTokenRefreshAttempt() {
        this.exampleResource = new OpowerClient.Builder<>(ExampleResource.class,
                                                          new SimpleUriProvider(RESOURCE_URI),
                                                          CLIENT_ID)
                .disableSensuPublishing()
                .disableMetrics()
                .clientSecret("foobar")
                .build();

        this.exampleResource.unauthorizedOnEvenRequests();
    }

    /**
     * Tests that mixin annotations work correctly when confgured on the ObjectMapper.
     */
    @Test
    public void testMixinAnnotations() {
        this.exampleResource = new OpowerClient.Builder<>(ExampleResource.class,
                new SimpleUriProvider(RESOURCE_URI),
                CLIENT_ID)
                .disableSensuPublishing()
                .disableMetrics()
                .configureObjectMapper(new ConfigurationCallback<ObjectMapper>() {
                    @Override
                    public void configure(ObjectMapper object) {
                        Map<Class<?>,Class<?>> typeMap =
                                ImmutableMap.<Class<?>,Class<?>>of(TestModelWithFields.class, TestModelMixin.class);
                        object.setMixInAnnotations(typeMap);
                    }
                })
                .build();

        TestModelWithFields testModel = this.exampleResource.testModelWithFields();
        assertEquals("mixin: name", testModel.getName());
    }
}
