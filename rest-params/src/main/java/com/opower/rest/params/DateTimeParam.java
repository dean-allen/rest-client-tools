package com.opower.rest.params;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * A parameter encapsulating date/time values. All non-parsable values will return a {@code 400 Bad
 * Request} response. All values returned are in UTC.
 *
 * @author coda.hale
 */
public class DateTimeParam extends AbstractParam<DateTime> {
    /**
     * Create a DateTimeParam instance based on the given date string.
     * @param input the date to use
     */
    public DateTimeParam(String input) {
        super(input);
    }

    @Override
    protected DateTime parse(String input) throws Exception {
        return new DateTime(input, DateTimeZone.UTC);
    }
}
