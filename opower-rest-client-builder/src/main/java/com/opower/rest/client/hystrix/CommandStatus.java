package com.opower.rest.client.hystrix;

import com.netflix.hystrix.HystrixCommandKey;
import com.opower.sensual.net.Check;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that contains the information needed to send a check to Sensu.
 * @author chris.phillips@opower.com
 */
public final class CommandStatus {
    private final HystrixCommandKey hystrixCommandKey;
    private final Check.Status status;
    private final String clientName;

    /**
     * Creates a CommandStatus which will be sent as a Sensu check.
     * @param clientName the name of the client
     * @param hystrixCommandKey the HystrixCommandKey to use
     * @param status the status of the check to be sent.
     */
    public CommandStatus(String clientName, HystrixCommandKey hystrixCommandKey, Check.Status status) {
        this.clientName = checkNotNull(clientName);
        this.hystrixCommandKey = checkNotNull(hystrixCommandKey);
        this.status = checkNotNull(status);
    }

    /**
     * The HystrixCommandKey which identifies the HystrixCommand for which the Sensu check is to be sent.
     * @return the HystrixCommandKey
     */
    public HystrixCommandKey getHystrixCommandKey() {
        return this.hystrixCommandKey;
    }

    /**
     * The Check.Status for this Sensu check.
     * @return the Status for the check
     */
    public Check.Status getStatus() {
        return this.status;
    }

    /**
     * The name of the client this check is for.
     * @return the name of the client.
     */
    public String getClientName() {
        return this.clientName;
    }

    /**
     * The message is dependant on the current Check.Status. The message will indicate that a short circuit
     * has occurred or that normal operations have resumed.
     * @return the message to send in the Sensu check.
     */
    public String getMessage() {
        switch (this.status) {
            case ok:
                return String.format("The client with id [%s] for command [%s] has resumed normal operations.",
                                     this.clientName, this.hystrixCommandKey.name());
            case critical:
                return String.format("The client with id [%s] for command [%s] has a tripped circuit breaker. Please investigate.",
                                     this.clientName, this.hystrixCommandKey.name());
            default:
                throw new IllegalArgumentException(this.status + " is not a valid status");
        }
    }
}
