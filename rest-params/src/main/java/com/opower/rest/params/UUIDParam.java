package com.opower.rest.params;

import java.util.UUID;

/**
 * A parameter encapsulating UUID values. All non-parsable values will return a {@code 400 Bad
 * Request} response.
 *
 * @author coda.hale
 */
public class UUIDParam extends AbstractParam<UUID> {

    /**
     * Creat a UUIDParam based on the given input string.
     * @param input the input string to use.
     */
    public UUIDParam(String input) {
        super(input);
    }

    /**
     * Create an IntParam instance based on the given integer.
     * @param value value represented by this instance
     */
    public UUIDParam(UUID value) {
        super(value);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return '"' + input + "\" is not a UUID.";
    }

    @Override
    protected UUID parse(String input) throws Exception {
        return UUID.fromString(input);
    }

}
