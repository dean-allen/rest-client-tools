package com.opower.rest.params;

/**
 * A parameter encapsulating integer values. All non-decimal values will return a
 * {@code 400 Bad Request} response.
 *
 * @author coda.hale
 */
public class IntParam extends AbstractParam<Integer> {
    /**
     * Create an IntParam instance based on the given string.
     * @param input the int string to use
     */
    public IntParam(String input) {
        super(input);
    }

    /**
     * Create an IntParam instance based on the given integer.
     * @param value value represented by this instance
     */
    public IntParam(int value) {
        super(value);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return '"' + input + "\" is not a number.";
    }

    @Override
    protected Integer parse(String input) {
        return Integer.valueOf(input);
    }
}
