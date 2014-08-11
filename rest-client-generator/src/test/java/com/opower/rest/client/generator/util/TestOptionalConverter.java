package com.opower.rest.client.generator.util;

import com.google.common.base.Optional;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link OptionalConverter}
 *
 * @author sachin.nene
 */
public class TestOptionalConverter {

    StringConverters.OptionalConverter converter = new StringConverters.OptionalConverter();

    @Test
    public void testOptionalPresent() {
        assertEquals("Return value of converter is unexpected", "1234", this.converter.toString(Optional.of(1234)));
    }

    @Test
    public void testOptionalPresentEmptyString() {
        assertEquals("Return value of converter is unexpected", "", this.converter.toString(Optional.of("")));
    }

    @Test
    public void testOptionalAbsent() {
        assertNull("Return value of converter should be null", this.converter.toString(Optional.absent()));
    }
}