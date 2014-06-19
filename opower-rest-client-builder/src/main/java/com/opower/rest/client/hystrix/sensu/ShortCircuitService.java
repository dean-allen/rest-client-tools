package com.opower.rest.client.hystrix.sensu;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.opower.rest.client.hystrix.CommandStatus;
import com.opower.rest.client.hystrix.CommandStatusListener;
import com.opower.sensual.PooledSensualClientHandler;
import com.opower.sensual.SensualPusher;
import com.opower.sensual.SensualPusherImpl;
import com.opower.sensual.net.Protocol;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Service that sends Sensu checks in the event of a short circuit.
 *
 * @author chris.phillips
 */
public class ShortCircuitService extends AbstractScheduledService implements CommandStatusListener {
    static final String SHORT_CIRCUIT = "short_circuit";
    static final String SHORT_CIRCUIT_HANDLER = "short_circuit_handler";
    private static final int SERVICE_INTERVAL = 5;
    private static final Logger LOG = LoggerFactory.getLogger(ShortCircuitService.class);

    private final ConcurrentLinkedQueue<CommandStatus> work = new ConcurrentLinkedQueue<>();
    private final SensualPusher sensualPusher;

    /**
     * Creates an instance of the service. This constructor is intended for testing purposes.
     * @param sensualPusher   the SensualPusher to use
     */
    protected ShortCircuitService(SensualPusher sensualPusher) {
        this.sensualPusher = checkNotNull(sensualPusher);
    }

    /**
     * Creates a ShortCircuitService with the default SensualPusher.
     */
    public  ShortCircuitService() {
        this(new SensualPusherImpl(new PooledSensualClientHandler(Protocol.UDP)));    
    }

    @Override
    protected void runOneIteration() throws Exception {
        try {
            Iterator<CommandStatus> iterator = this.work.iterator();

            while (iterator.hasNext()) {

                CommandStatus commandStatus = iterator.next();

                this.sensualPusher.pushCheck(SHORT_CIRCUIT,
                                             commandStatus.getMessage(),
                                             commandStatus.getStatus(),
                                             SHORT_CIRCUIT_HANDLER);
                iterator.remove();
            }
        } catch (Exception e) {
            LOG.warn("Failed publishing short circuit to sensu", e);
        }
    }

    @Override
    protected Scheduler scheduler() {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(SERVICE_INTERVAL, SERVICE_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Add the CommandStatus to the queue to be sent to Sensu.
     * @param commandStatus the CommandStatus to send
     */
    @Override
    public void handleCommandStatus(CommandStatus commandStatus) {
        this.work.add(commandStatus);
    }

    

}
