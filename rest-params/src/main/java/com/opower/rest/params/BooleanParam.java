package com.opower.rest.params;

/**
 * A parameter encapsulating boolean values. If the query parameter value is {@code "true"},
 * regardless of case, the returned value is {@link Boolean#TRUE}. If the query parameter value is
 * {@code "false"}, regardless of case, the returned value is {@link Boolean#FALSE}. All other
 * values will return a {@code 400 Bad Request} response.
 *
 * @author coda.hale
 */
public class BooleanParam extends AbstractParam<Boolean> {
    /**
     * BooleanParam representing the value 'true'.
     */
    public static final BooleanParam TRUE = new BooleanParam(true);

    /**
     * BooleanParam represeting the value 'false'.
     */
    public static final BooleanParam FALSE = new BooleanParam(false);

    /**
     * Create a BooleanParam based on the provided string.
     * @param input the input string is case insensitive.
     */
    public BooleanParam(String input) {
        super(input);
    }

    /**
     * Create a BooleanParam based on the provided boolean.
     * @param value value represented by this instance
     */
    public BooleanParam(boolean value) {
        super(value);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return '"' + input + "\" must be \"true\" or \"false\".";
    }

    @Override
    protected Boolean parse(String input) throws Exception {
        if ("true".equalsIgnoreCase(input)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(input)) {
            return Boolean.FALSE;
        }
        throw new Exception();
    }
}
