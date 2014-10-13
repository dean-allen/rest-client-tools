package com.opower.rest.params;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * Tests for the IntParam class.
 * @author coda.hale
 */
public class TestIntParam {

    /**
     * Valid integer strings produce an integer.
     * @throws Exception for convenience.
     */
    @Test
    public void anIntegerReturnsAnInteger() throws Exception {
        final IntParam param = new IntParam("200");

        assertThat(param.get())
                .isEqualTo(Response.Status.OK.getStatusCode());
    }

    /**
     * Non integer input strings throw exceptions.
     * @throws Exception for convenience
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void aNonIntegerThrowsAnException() throws Exception {
        try {
            new IntParam("foo");
            failBecauseExceptionWasNotThrown(WebApplicationException.class);
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();

            assertThat(response.getStatus())
                    .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

            assertThat(e.getMessage()).isEqualTo("Invalid parameter: foo");
        }
    }
}
