package com.opower.rest.exception;


/**
 * Represents a 500 HTTP Response.
 *
 * @author derrick.schneider@opower.com
 * @since 0.20
 */
public class InternalServerErrorException extends ErrorResponseException {

    private static final long serialVersionUID = -7640149613003312236L;

    /**
     * Constructor that passes 500 to its superclass.
     */
    public InternalServerErrorException() {
        super(SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Constructor that passes 500 to super, and sets the service error code and message.
     * @param serviceErrorCode the service-defined error code.
     * @param message an error message.
     */
    public InternalServerErrorException(String serviceErrorCode, String message) {
        super(SC_INTERNAL_SERVER_ERROR, serviceErrorCode, message);
    }

}