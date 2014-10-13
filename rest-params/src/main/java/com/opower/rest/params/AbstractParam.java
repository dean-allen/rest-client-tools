package com.opower.rest.params;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract base class from which to build Jersey parameter classes.
 *
 * @param <T> the type of value wrapped by the parameter
 * @author coda.hale
 */
public abstract class AbstractParam<T> {
    private final T value;

    /**
     * Given an input value from a client, creates a parameter wrapping its parsed value.
     *
     * @param input an input value from a client request
     */
    @SuppressWarnings({"AbstractMethodCallInConstructor", "OverriddenMethodCallDuringObjectConstruction" })
    protected AbstractParam(String input) {
        try {
            this.value = parse(checkNotNull(input));
        } catch (Exception e) {
            throw new InvalidParameterException(e, getErrorStatus(), input);
        }
    }

    /**
     * Returns the media type of the error message entity.
     *
     * @return the media type of the error message entity
     */
    protected MediaType mediaType() {
        return MediaType.APPLICATION_JSON_TYPE;
    }

    /**
     * Given a string representation which was unable to be parsed and the exception thrown, produce
     * an entity to be sent to the client.
     *
     * @param input the raw input value
     * @param e the exception thrown while parsing {@code input}
     * @return the error message to be sent the client
     */
    protected String errorMessage(String input, Exception e) {
        return String.format("Invalid parameter: %s (%s)", input, e.getMessage());
    }

    /**
     * Given a string representation which was unable to be parsed, produce a {@link Status} for the
     * Response to be sent to the client.
     *
     * @return the HTTP {@link Status} of the error message
     */
    @SuppressWarnings("MethodMayBeStatic")
    protected Status getErrorStatus() {
        return Status.BAD_REQUEST;
    }

    /**
     * Given a string representation, parse it and return an instance of the parameter type.
     *
     * @param input the raw input
     * @return {@code input}, parsed as an instance of {@code T}
     * @throws Exception if there is an error parsing the input
     */
    protected abstract T parse(String input) throws Exception;

    /**
     * Returns the underlying value.
     *
     * @return the underlying value
     */
    public T get() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if ((obj == null) || (getClass() != obj.getClass())) { return false; }
        final AbstractParam<?> that = (AbstractParam<?>) obj;
        return this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

}
