package com.opower.rest.params;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the DateTimeParam class.
 *
 * @author coda.hale
 */
public class TestDateTimeParam {

    private static final int TWENTY_TWELVE = 2012;
    private static final int ELEVEN = 11;
    private static final int NINETEEN = 19;

    /**
     * Correctly parses DateTime instances.
     * @throws Exception for convenience.
     */
    @Test
    public void parsesDateTimes() throws Exception {
        final DateTimeParam param = new DateTimeParam("2012-11-19");

        assertThat(param.get())
                .isEqualTo(new DateTime(TWENTY_TWELVE, ELEVEN, NINETEEN, 0, 0, DateTimeZone.UTC));
    }
}
