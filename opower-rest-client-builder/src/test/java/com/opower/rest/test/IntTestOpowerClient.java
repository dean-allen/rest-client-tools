package com.opower.rest.test;

import com.opower.rest.client.OpowerClient;
import com.opower.rest.client.generator.core.SimpleUriProvider;
import com.opower.rest.test.jetty.JettyRule;
import org.junit.ClassRule;
import org.junit.Test;

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
    private ExampleResource exampleResource;

    /**
     * Test that if a request to an endpoint fails with a 401 status code, the token is refreshed and another attempt is made.
     */
    @Test
    public void testAccessTokenRefreshAttempt() {
        this.exampleResource = new OpowerClient.Builder<>(ExampleResource.class,
                                                          new SimpleUriProvider("http://localhost:7000/"),
                                                          CLIENT_ID)
                .disableSensuPublishing()
                .disableMetrics()
                .clientSecret("foobar")
                .build();

        this.exampleResource.unauthorizedOnEvenRequests();
    }
}
