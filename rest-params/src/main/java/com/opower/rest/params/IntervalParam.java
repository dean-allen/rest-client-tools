package com.opower.rest.params;

import com.google.common.base.Preconditions;
import org.joda.time.Interval;

/**
 * Parses a String containing a <a href="http://en.wikipedia.org/wiki/ISO_8601#Time_intervals">ISO 8601 time interval</a> as an
 * {@link Interval} when used as a JAX-RS resource method parameter.
 * @author david.byron
 */
public class IntervalParam extends AbstractParam<Interval> {
    /**
     * Given an input value from a client, creates a parameter wrapping its parsed value.
     *
     * @param input an input value from a client request
     */
    public IntervalParam(String input) {
        super(input);
    }

    /**
     * Create an IntervalParam instance based on the given Interval.
     * @param value value represented by this instance
     */
    public IntervalParam(Interval value) {
        super(value);
    }

    @Override
    protected Interval parse(String input) throws Exception {
        // Passing a null string to Interval#parse results in an Interval that
        // starts and ends "now," but I don't want that, so bail.
        Preconditions.checkNotNull(input, "invalid null interval string");

        // NB: There isn't enough information in the input string to
        // unambiguously identify a timezone.  There's an offset from UTC, but
        // depending on daylight savings time, that's not enough.
        // Interval.parse sets the timezone to some default.
        return Interval.parse(input);
    }

    /**
     * Create a new instance of IntervalParam for a given {@link Interval}.
     *
     * @param interval An instance of Interval; can be null.
     * @return The new IntervalParam, or null if the parameter was null.
     */
    public static IntervalParam fromInterval(Interval interval) {
        // It's a little dicey to build a string only to parse it again, which
        // it basically what we're doing here.  We do this so methods can take
        // an Interval and call other methods that take an IntervalParam.
        return (interval == null) ? null : new IntervalParam(interval.toString());
    }
}
