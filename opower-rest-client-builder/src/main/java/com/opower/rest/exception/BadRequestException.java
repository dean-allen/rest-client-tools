package com.opower.rest.exception;

/**
 * Exception representing a 400 HTTP Response.
 *
 * @author derrick.schneider@opower.com
 * @since 0.20
 */
public class BadRequestException extends ErrorResponseException {

    private static final long serialVersionUID = -2318312239869331191L;

    /**
     * Constructor that passes 400 to super.
     */
    public BadRequestException() {
        super(SC_BAD_REQUEST);
    }

    /**
     * Constructor that passes 400 to super, and sets the service error code and message.
     * @param serviceErrorCode the service-defined error code.
     * @param message an error message.
     */
    public BadRequestException(String serviceErrorCode, String message) {
        super(SC_BAD_REQUEST, serviceErrorCode, message);
    }

}