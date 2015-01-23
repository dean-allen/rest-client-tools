package com.opower.rest.test;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.base.Throwables;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
    private static final int TIMEOUT = (int)TimeUnit.MINUTES.toMillis(10L);
    private AnnotatedFrobResource client;
    private MetricRegistry metricRegistry;

    /**
     * Tests that metrics appear in the codahale MetricRegistry like they should.
     */
    @Test
    public void metricsAreRecorded() {
        this.client = new OpowerClient.Builder<>(AnnotatedFrobResource.class,
                                                        new SimpleUriProvider("http://localhost:7000/"), 
                                                        CLIENT_ID)
                .disableSensuPublishing()
                .commandProperties(new ConfigurationCallback<HystrixCommandProperties.Setter>() {
                    @Override
                    public void configure(HystrixCommandProperties.Setter setter) {
                        setter.withExecutionIsolationThreadTimeoutInMilliseconds(TIMEOUT);
                    }
                })
                .build();
        
        this.client.findFrob(FROB_ID);
        this.client.updateFrob(FROB_ID, FROB_ID);
        this.client.createFrob(new Frob(FROB_ID));
        this.client.frobString(FROB_ID);

        try {
            this.client.frobJsonError();
        } catch (Exception ex) {
            // ignore
        }

        try {
            this.client.frobErrorResponse();
        } catch (Exception e) {
            // ignore
        }

        this.metricRegistry = SharedMetricRegistries.getOrCreate(String.format("%s.client",SERVICE_NAME));
        assertThat(this.metricRegistry.getTimers().size(), is(FROB_METHOD_COUNT));
        assertThat(this.metricRegistry.getCounters().size(), is(1));

        Properties props = new Properties();
        // force all the circuit breakers open
        for (Method method : FrobResource.class.getMethods()) {
            
            props.put(String.format("hystrix.command.%s.circuitBreaker.forceOpen", 
                                    HystrixClient.Builder.keyForMethod(method).name()), true);
        }
        
        ConfigurationManager.loadProperties(props);

        checkMetrics("findFrob", FROB_ID);
        checkMetrics("updateFrob", FROB_ID, FROB_ID);
        checkMetrics("createFrob", new Frob(FROB_ID));
        checkMetrics("frobString", FROB_ID);
        checkMetrics("frobJsonError");
        checkMetrics("frobErrorResponse");
    }

    /**
     *
     */
    private void checkMetrics(String methodName, Object... args) {
        try {
            Class[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            try {
                FrobResource.class.getDeclaredMethod(methodName, paramTypes).invoke(this.client, args);
            } catch (NoSuchMethodException | IllegalAccessException  ex) {
                Throwables.propagate(ex);
            } catch (InvocationTargetException ex) {
                Throwables.propagate(ex.getTargetException());
            }
            fail();
        } catch (HystrixRuntimeException e) {
            assertThat(e.getFailureType(), is(HystrixRuntimeException.FailureType.SHORTCIRCUIT));
            checkFrobMethod(methodName);
        }
    }

    private void checkFrobMethod(String methodName) {
        String timerName = String.format("%s.%s", FrobResource.class.getCanonicalName(), methodName);
        assertThat(this.metricRegistry.getTimers().get(timerName), notNullValue());
        String exceptionCounterName = timerName + "-Exception";
        assertThat(this.metricRegistry.getCounters().get(exceptionCounterName), notNullValue());
        String gaugeName = timerName + ".circuitbreaker.open";
        assertThat((int)this.metricRegistry.getGauges().get(gaugeName).getValue(), is(1));
    }
    
   
}
