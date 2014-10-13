package com.opower.rest.params;

import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * Tests for the UUIDParam class.
 *
 * @author coda.hale
 */
public class TestUUIDParam {

    /**
     * UUID string returns a UUID.
     * @throws Exception for convenience
     */
    @Test
    public void aUUIDStringReturnsAUUIDObject() throws Exception {
        final String uuidString = "067e6162-3b6f-4ae2-a171-2470b63dff00";
        final UUID uuid = UUID.fromString(uuidString);

        final UUIDParam param = new UUIDParam(uuidString);
        assertThat(param.get())
                .isEqualTo(uuid);
    }

    /**
     * Non UUID string throws exception.
     * @throws Exception for convenience
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void aNonUUIDThrowsAnException() throws Exception {
        try {
            new UUIDParam("foo");
            failBecauseExceptionWasNotThrown(WebApplicationException.class);
        } catch (WebApplicationException e) {
            final Response response = e.getResponse();

            assertThat(response.getStatus())
                    .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());

            assertThat(e.getMessage()).isEqualTo("Invalid parameter: foo");
        }
    }
}
