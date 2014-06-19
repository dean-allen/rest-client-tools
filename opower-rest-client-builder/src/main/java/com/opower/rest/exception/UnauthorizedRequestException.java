package com.opower.rest.exception;


/**
 * Represents a 500 HTTP Response.
 *
 * @author matthew.greenfield@opower.com
 */
public class UnauthorizedRequestException extends ErrorResponseException {

    private static final long serialVersionUID = -1005357397893362291L;

    /**
     * Constructor that passes 401 to super.
     */
    public UnauthorizedRequestException() {
        super(SC_BAD_REQUEST);
    }

    /**
     * Constructor that passes 401 to super, and sets the service error code and message.
     *
     * @param serviceErrorCode the service-defined error code.
     * @param message an error message.
     */
    public UnauthorizedRequestException(String serviceErrorCode, String message) {
        super(SC_UNAUTHORIZED, serviceErrorCode, message);
    }
}
