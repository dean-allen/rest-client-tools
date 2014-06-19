package com.opower.rest.exception;

/**
 * Exception representing a Not Found HTTP error response.
 *
 * @author derrick.schneider@opower.com
 * @since 0.20
 */
public class NotFoundException extends ErrorResponseException {

    private static final long serialVersionUID = 3196098585101182816L;

    /**
     * Passes 404 to ErrorResponseException.
     */
    public NotFoundException() {
        super(SC_NOT_FOUND);
    }

    /**
     * Constructor that passes 404 to super, and sets the service error code and message.
     * @param serviceErrorCode the service-defined error code.
     * @param message an error message.
     */
    public NotFoundException(String serviceErrorCode, String message) {
        super(SC_NOT_FOUND, serviceErrorCode, message);
    }

}