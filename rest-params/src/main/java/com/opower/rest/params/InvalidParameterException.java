package com.opower.rest.params;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exception thrown when a request parameter cannot be parsed.
 *
 * @author chris.barrett
 * @since 0.3.2
 */
public class InvalidParameterException extends WebApplicationException {
    public static final long serialVersionUID = 3870879101180315791L;

    private final String input;

    /**
     * Create a new instance.
     *
     * @param cause The error thrown from being unable to parse the request parameter.
     * @param status The HTTP status for the response.
     * @param input The text of the request parameter that could not be parsed; may be null.
     */
    public InvalidParameterException(Throwable cause, Response.Status status, String input) {
        super(checkNotNull(cause), checkNotNull(status));
        this.input = input;
    }

    @Override
    public String getMessage() {
        return String.format("Invalid parameter: %s", this.input);
    }
}
