package com.opower.rest.test;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.opower.rest.ResourceMetadata;
import com.opower.rest.client.ConfigurationCallback;
import com.opower.rest.client.OpowerClient;
import com.opower.rest.client.generator.core.SimpleUriProvider;
import com.opower.rest.client.generator.hystrix.HystrixClient;
import com.opower.rest.test.jetty.JettyRule;
import com.opower.rest.test.resource.Frob;
import com.opower.rest.test.resource.FrobResource;
import java.lang.reflect.Method;
import java.util.Properties;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for the integration with the codahale-3-metrics-provider.
 * @author chris.phillips
 */
public class Codahale3MetricsIntegrationTesting {
    
    public static final int PORT = 7000;
    @ClassRule
    public static final JettyRule JETTY_RULE = new JettyRule(PORT, Frob.class.getResource("/jersey/1/web.xml").toString());
    private static final ResourceMetadata RESOURCE_METADATA = AnnotatedFrobResource.class.getAnnotation(ResourceMetadata.class);
    private static final String SERVICE_NAME = String.format("%s-v%d",RESOURCE_METADATA.serviceName(),
                                                             RESOURCE_METADATA.serviceVersion());
    private static final String CLIENT_ID = "client.id";
    private static final String FROB_ID = "frobId";
    private static final int FROB_METHOD_COUNT = FrobResource.class.getDeclaredMethods().length;

    /**
     * Tests that metrics appear in the codahale MetricRegistry like they should.
     */
    @Test
    public void metricsAreRecorded() {
        AnnotatedFrobResource client = new OpowerClient.Builder<>(AnnotatedFrobResource.class,
                                                        new SimpleUriProvider("http://localhost:7000/"), 
                                                        CLIENT_ID)
                .disableSensuPublishing()
                .commandProperties(new ConfigurationCallback<HystrixCommandProperties.Setter>() {
                    @Override
                    public void configure(HystrixCommandProperties.Setter setter) {
                        setter.withExecutionIsolationThreadTimeoutInMilliseconds(PORT);
                    }
                })
                .build();
        
        client.findFrob(FROB_ID);
        client.updateFrob(FROB_ID, FROB_ID);
        client.createFrob(new Frob(FROB_ID));
        client.frobString(FROB_ID);

        try {
            client.frobJsonError();
        } catch (Exception ex) {
            // ignore
        }

        try {
            client.frobErrorResponse();
        } catch (Exception e) {
            // ignore
        }

        MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(String.format("%s.client",SERVICE_NAME));
        assertThat(metricRegistry.getTimers().size(), is(FROB_METHOD_COUNT));
        assertThat(metricRegistry.getCounters().size(), is(1));


        Properties props = new Properties();
        // force all the circuit breakers open
        for (Method method : FrobResource.class.getMethods()) {
            
            props.put(String.format("hystrix.command.%s.circuitBreaker.forceOpen", 
                                    HystrixClient.Builder.keyForMethod(method).name()), true);
        }
        
        ConfigurationManager.loadProperties(props);

        try {
            client.findFrob(FROB_ID);
            checkFrobMethod(metricRegistry, "findFrob");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getFailureType(), is(HystrixRuntimeException.FailureType.SHORTCIRCUIT));
        }
        try {
            client.updateFrob(FROB_ID, FROB_ID);
            checkFrobMethod(metricRegistry, "updateFrob");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getFailureType(), is(HystrixRuntimeException.FailureType.SHORTCIRCUIT));
        }
        try {
            client.createFrob(new Frob(FROB_ID));
            checkFrobMethod(metricRegistry, "createFrob");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getFailureType(), is(HystrixRuntimeException.FailureType.SHORTCIRCUIT));
        }
        try {
            client.frobString(FROB_ID);
            checkFrobMethod(metricRegistry, "frobString");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getFailureType(), is(HystrixRuntimeException.FailureType.SHORTCIRCUIT));
        }
        try {
            client.frobJsonError();
            checkFrobMethod(metricRegistry, "frobJsonError");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getFailureType(), is(HystrixRuntimeException.FailureType.SHORTCIRCUIT));
        }
        try {
            client.frobErrorResponse();
            checkFrobMethod(metricRegistry, "frobErrorResponse");
        } catch (HystrixRuntimeException e) {
            assertThat(e.getFailureType(), is(HystrixRuntimeException.FailureType.SHORTCIRCUIT));
        }


    }

    private void checkFrobMethod(MetricRegistry metricRegistry, String methodName) {
        String timerName = String.format("%s.%s", FrobResource.class.getCanonicalName(), methodName);
        assertThat(metricRegistry.getTimers().get(timerName).getCount(), is(1L));
        String exceptionCounterName = timerName + ".exceptions";
        assertThat(metricRegistry.getCounters().get(exceptionCounterName).getCount(), is(1L));
        String gaugeName = timerName + ".circuitbreaker.open";
        assertThat((int)metricRegistry.getGauges().get(gaugeName).getValue(), is(1));
        String shortCircuitCounterName = timerName + ".shortcircuit.count";
        assertThat(metricRegistry.getCounters().get(shortCircuitCounterName).getCount(), is(1L));
                
    }
    
   
}
