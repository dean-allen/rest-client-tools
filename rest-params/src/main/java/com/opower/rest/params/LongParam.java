package com.opower.rest.params;

/**
 * A parameter encapsulating long values. All non-decimal values will return a {@code 400 Bad
 * Request} response.
 *
 * @author coda.hale
 */
public class LongParam extends AbstractParam<Long> {
    /**
     * Create a LongParam instance based on the given string.
     * @param input the input string to use
     */
    public LongParam(String input) {
        super(input);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return '"' + input + "\" is not a number.";
    }

    @Override
    protected Long parse(String input) {
        return Long.valueOf(input);
    }
}
