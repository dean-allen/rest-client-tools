package com.opower.rest.params;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * Tests for the LongParam class.
 * @author coda.hale
 */
public class TestLongParam {

    private static final long TWO_HUNDO = 200L;

    /**
     * Long string results in a Long.
     * @throws Exception for convenience
     */
    @Test
    public void aLongReturnsALong() throws Exception {
        final LongParam param = new LongParam("200");

        assertThat(param.get())
                .isEqualTo(TWO_HUNDO);
    }

    /**
     * Invalid long string throws an exception.
     * @throws Exception for convenience
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void aNonIntegerThrowsAnException() throws Exception {
        try {
            new LongParam("foo");
            failBecauseExceptionWasNotThrown(WebApplicationException.class);
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();
            assertThat(response.getStatus())
                    .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(e.getMessage()).isEqualTo("Invalid parameter: foo");
        }
    }
}
