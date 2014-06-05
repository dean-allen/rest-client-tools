package com.opower.rest.exception;


/**
 * Represents a 410 HTTP error response.
 *
 * @author derrick.schneider@opower.com
 * @since 0.20
 */
public class GoneException extends ErrorResponseException {

    private static final long serialVersionUID = 7228168983239582585L;

    /**
     * Constructor that passes 410 to superclass.
     */
    public GoneException() {
        super(SC_GONE);
    }

    /**
     * Constructor that passes 410 to super, and sets the service error code and message.
     * @param serviceErrorCode the service-defined error code.
     * @param message an error message.
     */
    public GoneException(String serviceErrorCode, String message) {
        super(SC_GONE, serviceErrorCode, message);
    }

}