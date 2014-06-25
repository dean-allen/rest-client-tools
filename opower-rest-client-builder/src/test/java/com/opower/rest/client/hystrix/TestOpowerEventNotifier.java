package com.opower.rest.client.hystrix;

import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.HystrixCommandKey;
import com.opower.metrics.MetricsProvider;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;

/**
 * Unit tests for the OpowerEventNotifier. There is some sensitive code that requires thread-safety
 * here.
 * @author chris.phillips
 */
@RunWith(EasyMockRunner.class)
public class TestOpowerEventNotifier extends EasyMockSupport {

    private static final HystrixCommandKey KEY = HystrixCommandKey.Factory.asKey("testKey");
    private static final String CLIENT_NAME = "clientName";
    private static final String COUNTER_NAME = String.format("%s.shortcircuit.count", KEY.name());
    private static final String GAUGE_NAME = String.format("%s.circuitbreaker.open", KEY.name());
    
    private OpowerEventNotifier opowerEventNotifier = new OpowerEventNotifier();
    private MetricsProvider metricsProvider;

    /**
     * Assembles the dependencies.
     */
    @Before
    public void setUp() {
        this.metricsProvider = createStrictMock(MetricsProvider.class);
        this.metricsProvider.gauge(eq(GAUGE_NAME), anyObject(MetricsProvider.Gauge.class));
        expectLastCall();
    }

    /**
     * Resets the internal maps in between tests.
     */
    @After
    public void after() {
        OpowerEventNotifier.resetForTesting();
    }

    /**
     * If the first time an event for a Command is a SHORT_CIRCUITED type then we mark a short circuit.
     */
    @Test
    public void firstShortCircuitGetsMarked() {
        this.metricsProvider.mark(COUNTER_NAME);
        expectLastCall().once();
        replayAll();
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processShortCircuit(KEY);
        
        verifyAll();
    }

    /**
     * A short circuit only is counted if there was a change from closed to open.
     */
    @Test
    public void consecutiveShortCircuitStatusesMarksOnce() {
        this.metricsProvider.mark(COUNTER_NAME);
        expectLastCall().once();
        replayAll();
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processShortCircuit(KEY);
        this.opowerEventNotifier.processShortCircuit(KEY);
        verifyAll();    
    }

    /**
     * When a short-circuit occurs a Status is sent.
     */
    @Test
    public void changeFromClosedToOpenSendsOneStatus() {
        this.metricsProvider.mark(COUNTER_NAME);
        replayAll();

        // set the status for the key to show it currently closed
        OpowerEventNotifier.primeForTesting(ImmutableMap.of(KEY, false));
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processShortCircuit(KEY);
        this.opowerEventNotifier.processShortCircuit(KEY);
        verifyAll();
    }
    
}
