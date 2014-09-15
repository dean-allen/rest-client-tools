package com.opower.rest.params;

import org.joda.time.LocalDate;

/**
 * A parameter encapsulating local date values. All non-parsable values will return a {@code 400 Bad
 * Request} response.
 *
 * @author coda.hale
 */
public class LocalDateParam extends AbstractParam<LocalDate> {
    /**
     * Create a LocalDateParam instance based on the given local date string.
     * @param input the string to use
     */
    public LocalDateParam(String input) {
        super(input);
    }

    @Override
    protected LocalDate parse(String input) throws Exception {
        return new LocalDate(input);
    }
}
