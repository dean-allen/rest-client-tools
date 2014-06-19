package com.opower.rest.client.hystrix.sensu;

import com.netflix.hystrix.HystrixCommandKey;
import com.opower.rest.client.hystrix.CommandStatus;
import com.opower.sensual.SensualPusher;
import com.opower.sensual.net.Check;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expectLastCall;

/**
 * Tests for the ShortCircuitService.
 * @author chris.phillips
 */
@RunWith(EasyMockRunner.class)
public class TestShortCircuitService extends EasyMockSupport {
    
    private static final String CLIENT_NAME = "clientName";
    private static final HystrixCommandKey COMMAND_KEY = HystrixCommandKey.Factory.asKey("testKey");
    
    private SensualPusher sensualPusher;
    private ShortCircuitService shortCircuitService;

    /**
     * Sets up the fixture.
     */
    @Before
    public void setUp() {
        this.sensualPusher = createStrictMock(SensualPusher.class);
        this.shortCircuitService = new ShortCircuitService(this.sensualPusher);
    }

    /**
     * Make sure an empty queue does nothing.
     * @throws Exception for convenience
     */
    @Test
    public void emptyQueueDoesNothing() throws Exception {
        this.shortCircuitService.runOneIteration();
        replayAll();
        verifyAll();
    }

    /**
     * Makes sure the queue is emptied when the service runs.
     * @throws Exception for convenience
     */
    @Test
    public void allPendingWorkIsProcessed() throws Exception {
        CommandStatus critical = new CommandStatus(CLIENT_NAME, COMMAND_KEY, Check.Status.critical);
        CommandStatus ok = new CommandStatus(CLIENT_NAME, COMMAND_KEY, Check.Status.ok);
        this.shortCircuitService.handleCommandStatus(critical);  
        this.shortCircuitService.handleCommandStatus(ok);
        
        this.sensualPusher.pushCheck(ShortCircuitService.SHORT_CIRCUIT, critical.getMessage(), critical.getStatus(),
                                ShortCircuitService.SHORT_CIRCUIT_HANDLER);
        expectLastCall();
        this.sensualPusher.pushCheck(ShortCircuitService.SHORT_CIRCUIT, ok.getMessage(), ok.getStatus(),
                                ShortCircuitService.SHORT_CIRCUIT_HANDLER);
        expectLastCall();
        replayAll();
        this.shortCircuitService.runOneIteration();
        verifyAll();
    }
}
