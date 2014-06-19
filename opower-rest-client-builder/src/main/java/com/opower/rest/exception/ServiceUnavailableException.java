package com.opower.rest.exception;

/**
 *  Represents a 503 Service Unavailable HTTP Response.
 *
 * @author derrick.schneider@opower.com
 * @since 0.20
 */
public class ServiceUnavailableException extends ErrorResponseException {

    private static final long serialVersionUID = -2202883518808689634L;

    /**
     * Constructor that passes 503 to superclass.
     */
    public ServiceUnavailableException() {
        super(SC_SERVICE_UNAVAILABLE);
    }

    /**
     * Constructor that passes 503 to super, and sets the service error code and message.
     * @param serviceErrorCode the service-defined error code.
     * @param message an error message.
     */
    public ServiceUnavailableException(String serviceErrorCode, String message) {
        super(SC_SERVICE_UNAVAILABLE, serviceErrorCode, message);
    }

}