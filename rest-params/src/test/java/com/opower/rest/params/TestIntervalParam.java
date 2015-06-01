package com.opower.rest.params;

import javax.ws.rs.WebApplicationException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link IntervalParam}.
 *
 * @author david.byron
 * @since 0.3.2
 */
public class TestIntervalParam {
    // An arbitrary interval
    private static final Interval TEST_INTERVAL = new Interval(new DateTime(2014, 1, 1, 5, 19,
                                                                            DateTimeZone.forID("America/Chicago")),
                                                               new DateTime(2014, 6, 15, 19, 11,
                                                                            DateTimeZone.forID("America/Los_Angeles")));

    @Rule
    public ExpectedException testRuleException = ExpectedException.none();

    /**
     * Test that {@link IntervalParam#IntervalParam(String)} correctly parses the argument as a {@link Interval} given a
     * String containing a non-null, valid, correctly formatted date.
     */
    @Test
    public void testStringConstructor() {
        assertTrue(compareIntervals(TEST_INTERVAL, new IntervalParam(TEST_INTERVAL.toString()).get()));
    }

    /**
     * Test that {@link IntervalParam#IntervalParam(String)} throws {@link WebApplicationException} when given a null String.
     */
    @Test
    public void testStringConstructorNullValue() {
        this.testRuleException.expect(WebApplicationException.class);
        final IntervalParam retval = new IntervalParam((String)null);
    }

    /**
     * Test that {@link IntervalParam#IntervalParam(String)} throws {@link WebApplicationException} when given a String
     * containing an incorrectly formatted date.
     */
    @Test
    public void testStringConstructorInvalidFormat() {
        this.testRuleException.expect(WebApplicationException.class);
        new IntervalParam("20140101");
    }

    /**
     * Test that {@link IntervalParam#IntervalParam(String)} throws {@link WebApplicationException} when given a String
     * containing a correctly formatted but invalid date.
     */
    @Test
    public void testStringConstructorInvalidDate() {
        this.testRuleException.expect(WebApplicationException.class);
        new IntervalParam("2014-02-31");
    }

    /**
     * Test that {@link IntervalParam#fromInterval(org.joda.time.Interval)} returns a new instance of IntervalParam with the
     * correct {@link Interval}.  This tests {@link IntervalParam#toString()} along the way.  It doesn't have its own test
     * since time zones can change in the parse/toString process so comparing against TEST_INTERVAL.toString doesn't work.
     */
    @Test
    public void testFromInterval() {
        assertTrue(compareIntervals(TEST_INTERVAL, IntervalParam.fromInterval(TEST_INTERVAL).get()));
    }

    /**
     * Test that {@link IntervalParam#fromInterval(org.joda.time.Interval)} returns null when given a null {@link Interval}.
     */
    @Test
    public void testFromIntervalNullValue() {
        assertNull(IntervalParam.fromInterval(null));
    }

    /**
     * Interval.equals compares including time zone information, and <a
     * href="http://joda-time.sourceforge.net/apidocs/org/joda/time/base/AbstractInterval.html#toString()">Interval.toString</a>
     * uses the default time zone.  So, our hand-constructed Interval for testing likely has a different time zone than an
     * Interval we've built by parsing the output of toString.  As long as the start and times are the same instant, we're OK.
     * A newer version of joda already includes
     * <a href="https://github.com/JodaOrg/joda-time/blob/master/src/main/java/org/joda/time/base/AbstractInterval.java#L253">
     * this method</a>, but upgrading feels painful.
     */
    private boolean compareIntervals(Interval a, Interval b) {
        return (a.getStartMillis() == b.getStartMillis())
            && (a.getEndMillis() == b.getEndMillis());
    }
}
