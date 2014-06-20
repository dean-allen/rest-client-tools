package com.opower.rest.client.hystrix;

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.opower.metrics.MetricsProvider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HystrixEventNotifier implementation that collects information on SHORT_CIRCUITED events. The checks are
 * sent to Sensu asynchronously since this class has to be thread safe and fast in the way it handles events.
 * @author chris.phillips
 */
public class OpowerEventNotifier extends HystrixEventNotifier {

    /**
     * Visible for testing.
     */
    static final Boolean CIRCUIT_OPEN = Boolean.TRUE;
    /**
     * Visible for testing.
     */
    static final Boolean CIRCUIT_CLOSED = Boolean.FALSE;
    private static final Logger LOG = LoggerFactory.getLogger(OpowerEventNotifier.class);

    private static final ConcurrentMap<HystrixCommandKey, Boolean> CIRCUIT_BREAKER_STATUS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<HystrixCommandKey, String> CLIENT_NAMES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<HystrixCommandKey, MetricsProvider> METRICS_PROVIDERS = new ConcurrentHashMap<>();

    @Override
    public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
        switch (eventType) {
            case SHORT_CIRCUITED:
                processShortCircuit(key);
                break;
            default:
                // do nothing for other events
        }
    }

    /**
     * Makes sure to only send a CommandStatus if the circuit breaker is changing from closed to open.
     * Visible for testing.
     * @param key the HystrixCommandKey to use.
     */
    void processShortCircuit(HystrixCommandKey key) {
        Boolean currentState = CIRCUIT_BREAKER_STATUS.putIfAbsent(key, CIRCUIT_OPEN);
        // if the circuit breaker hasn't been seen before or if the circuit is
        // currently closed try to mark it open and send the event
        if (currentState == null) {
            // we marked the circuitbreaker as open
            markShortCircuit(key, HystrixEventType.SHORT_CIRCUITED);
        } else if (currentState.equals(CIRCUIT_CLOSED)) {
            // only send the event if we were the thread to actually update the value
            if (CIRCUIT_BREAKER_STATUS.replace(key, currentState, CIRCUIT_OPEN)) {
                markShortCircuit(key, HystrixEventType.SHORT_CIRCUITED);
            }
        }
    }

    private void markShortCircuit(HystrixCommandKey key, HystrixEventType eventType) {
        MetricsProvider metricsProvider = METRICS_PROVIDERS.get(key);
        String clientName = CLIENT_NAMES.get(key);
        if (metricsProvider != null && clientName != null) {
            metricsProvider.mark(String.format("%s.shortcircuit.count",
                                               key.name()));
        } else {
            LOG.warn(String.format("Improperly registered client found name: [%s] and metricsProvider [%s]. Both must be non-null",
                                   clientName, metricsProvider));
        }
    }

    /**
     * Registers a client instance so that when Hystrix short circuits occur, a check will be sent to Sensu and the history of
     * the circuit breaker will be tracked via the provided MetricsProvider.
     *
     * @param key             the HystrixCommandKey to use. Note that if you create two instances using the same 
     *                        ResourceInterface but different
     *                        client names, then their statistics will be merged together and the clientName under which they
     *                        will appear is
     *                        undefined.
     * @param clientName      the name of the client instance. Used to disambiguate the various
     * @param metricsProvider the MetricsProvider instance for this client
     */
    public static void registerClient(final HystrixCommandKey key, String clientName, MetricsProvider metricsProvider) {
        checkArgument(clientName != null && clientName.trim().length() > 0);
        CLIENT_NAMES.putIfAbsent(checkNotNull(key), clientName);
        METRICS_PROVIDERS.putIfAbsent(checkNotNull(key), checkNotNull(metricsProvider));


        metricsProvider.gauge(String.format("%s.circuitbreaker.open",
                                            key.name()),
                new MetricsProvider.Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        HystrixCircuitBreaker hcb = HystrixCircuitBreaker.Factory.getInstance(key);
                        if (hcb == null) {
                            return 0;
                        } else {
                            return hcb.isOpen() ? 1 : 0;
                        }
                    }
                });
    }

    /**
     * This method will reset the internal maps in between tests.
     */
    static void resetForTesting() {
        CIRCUIT_BREAKER_STATUS.clear();
        CLIENT_NAMES.clear();
        METRICS_PROVIDERS.clear();
    }

    /**
     * Initializes the internal CIRCUIT_BREAKER_STATUS map with the given values. This method is for testing
     * only, hence the visibility.
     * @param initialTestValues the values to initialize the internal map with
     */
    static void primeForTesting(Map<HystrixCommandKey, Boolean> initialTestValues) {
        CIRCUIT_BREAKER_STATUS.putAll(initialTestValues);
    }
}
