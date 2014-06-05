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
import static org.easymock.EasyMock.anyString;
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
    
    private CommandStatusListener shortCircuitService;
    private OpowerEventNotifier opowerEventNotifier;
    private MetricsProvider metricsProvider;

    /**
     * Assembles the dependencies.
     */
    @Before
    public void setUp() {
        this.shortCircuitService = createStrictMock(CommandStatusListener.class);
        this.opowerEventNotifier = new OpowerEventNotifier(this.shortCircuitService);
        this.metricsProvider = createNiceMock(MetricsProvider.class);
        this.metricsProvider.gauge(anyString(), anyObject(MetricsProvider.Gauge.class));
    }

    /**
     * Resets the internal maps in between tests.
     */
    @After
    public void after() {
        OpowerEventNotifier.resetForTesting();
    }

    /**
     * If the first time an event for a Command is a SHORT_CIRCUITED type, then a CommandStatus
     * should be sent.
     */
    @Test
    public void firstShortCircuitSendsStatus() {
        this.shortCircuitService.handleCommandStatus(anyObject(CommandStatus.class));
        expectLastCall();
        
        replayAll();
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processShortCircuit(KEY);
        
        verifyAll();
    }

    /**
     * If multiple SHORT_CIRCUITED events are processed in a row, only one CommandStatus should
     * be sent.
     */
    @Test
    public void multipleShortCircuitSendsOneStatus() {
        this.shortCircuitService.handleCommandStatus(anyObject(CommandStatus.class));
        expectLastCall().once();
        replayAll();
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processShortCircuit(KEY);
        this.opowerEventNotifier.processShortCircuit(KEY);
        verifyAll();    
    }

    /**
     * The first time a SUCCESS event is processed nothing should happen.
     */
    @Test
    public void firstSuccessEventDoesNothing() {
        replayAll();
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processSuccess(KEY);
        verifyAll();
    }

    /**
     * If multiple SUCCESS events in a row are processed, nothing should happen.
     */
    @Test
    public void multipleSuccessEventsDoesNothing() {
        replayAll();
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processSuccess(KEY);
        this.opowerEventNotifier.processSuccess(KEY);
        this.opowerEventNotifier.processSuccess(KEY);
        verifyAll();
    }

    /**
     * When a circuit resumes normal operation a Status should be sent.
     */
    @Test
    public void changeFromOpenToClosedSendsOneStatus() {
        this.shortCircuitService.handleCommandStatus(anyObject(CommandStatus.class));
        expectLastCall();
        replayAll();
        
        OpowerEventNotifier.primeForTesting(ImmutableMap.of(KEY, true));
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processSuccess(KEY);
        this.opowerEventNotifier.processSuccess(KEY);
        verifyAll();    
    }

    /**
     * When a short-circuit occurs a Status is sent.
     */
    @Test
    public void changeFromClosedToOpenSendsOneStatus() {
        this.shortCircuitService.handleCommandStatus(anyObject(CommandStatus.class));
        expectLastCall();
        replayAll();

        OpowerEventNotifier.primeForTesting(ImmutableMap.of(KEY, false));
        OpowerEventNotifier.registerClient(KEY, CLIENT_NAME, this.metricsProvider);
        this.opowerEventNotifier.processShortCircuit(KEY);
        this.opowerEventNotifier.processShortCircuit(KEY);
        verifyAll();
    }
    
}
