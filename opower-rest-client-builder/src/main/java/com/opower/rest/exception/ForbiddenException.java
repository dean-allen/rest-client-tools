package com.opower.rest.exception;

/**
 * Represents a 403 Forbidden HTTP response.
 *
 * @author derrick.schneider@opower.com
 * @since 0.20
 */
public class ForbiddenException extends ErrorResponseException {

    private static final long serialVersionUID = -3329444681897217328L;

    /**
     * Constructor that passes 403 to superclass.
     */
    public ForbiddenException() {
        super(SC_FORBIDDEN);
    }

    /**
     * Constructor that passes 403 to super, and sets the service error code and message.
     * @param serviceErrorCode the service-defined error code.
     * @param message an error message.
     */
    public ForbiddenException(String serviceErrorCode, String message) {
        super(SC_FORBIDDEN, serviceErrorCode, message);
    }

}