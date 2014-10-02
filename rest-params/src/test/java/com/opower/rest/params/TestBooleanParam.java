package com.opower.rest.params;


import com.opower.rest.params.AbstractParam.ErrorMessage;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * Tests for the BooleanParam class.
 * @author coda.hale
 */
public class TestBooleanParam {

    /**
     * Lower case true returns true.
     * @throws Exception for convenience
     */
    @Test
    public void trueReturnsTrue() throws Exception {
        final BooleanParam param = new BooleanParam("true");

        assertThat(param.get())
                .isTrue();
    }

    /**
     * Upper case TRUE returns true.
     * @throws Exception for convenience
     */
    @Test
    public void uppercaseTrueReturnsTrue() throws Exception {
        final BooleanParam param = new BooleanParam("TRUE");

        assertThat(param.get())
                .isTrue();
    }

    /**
     * Lower case false returns false.
     * @throws Exception for convenience.
     */
    @Test
    public void falseReturnsFalse() throws Exception {
        final BooleanParam param = new BooleanParam("false");

        assertThat(param.get())
                .isFalse();
    }

    /**
     * Upper case FALSE returns false.
     * @throws Exception for convenience
     */
    @Test
    public void uppercaseFalseReturnsFalse() throws Exception {
        final BooleanParam param = new BooleanParam("FALSE");

        assertThat(param.get())
                .isFalse();
    }

    /**
     * Null input throws an exception.
     * @throws Exception for convenience.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void nullThrowsAnException() throws Exception {
        try {
            new BooleanParam(null);
            failBecauseExceptionWasNotThrown(WebApplicationException.class);
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();

            assertThat(response.getStatus())
                    .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

            ErrorMessage entity = (ErrorMessage) response.getEntity();
            assertThat(entity.getCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(entity.getMessage())
                    .isEqualTo("\"null\" must be \"true\" or \"false\".");
        }
    }

    /**
     * If a non boolean string is passed as input and exception is thrown.
     * @throws Exception for convenience.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void nonBooleanValuesThrowAnException() throws Exception {
        try {
            new BooleanParam("foo");
            failBecauseExceptionWasNotThrown(WebApplicationException.class);
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();

            assertThat(response.getStatus())
                    .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

            ErrorMessage entity = (ErrorMessage) response.getEntity();
            assertThat(entity.getCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
            assertThat(entity.getMessage())
                    .isEqualTo("\"foo\" must be \"true\" or \"false\".");
        }
    }
}
