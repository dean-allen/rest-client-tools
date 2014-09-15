package com.opower.rest.params;

import org.joda.time.LocalDate;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the LocalDateParam class.
 *
 * @author coda.hale
 */
public class TestLocalDateParam {

    private static final int TWENTY_TWELVE = 2012;
    private static final int ELEVEN = 11;
    private static final int NINETEEN = 19;

    /**
     * LocalDates are parsed correctly.
     * @throws Exception for convenience.
     */
    @Test
    public void parsesLocalDates() throws Exception {
        final LocalDateParam param = new LocalDateParam("2012-11-19");

        assertThat(param.get())
                .isEqualTo(new LocalDate(TWENTY_TWELVE, ELEVEN, NINETEEN));
    }
}
