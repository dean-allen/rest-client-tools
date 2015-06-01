package com.opower.rest.test;

import com.opower.rest.client.model.TestModelWithFields;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Implementation of {@link com.opower.rest.test.ExampleResource}.
 *
 * @author matthew.greenfield
 */
public class ExampleServerResource implements ExampleResource {
    private int requestCount;

    @Override
    public String unauthorizedOnEvenRequests() {
        int mod = this.requestCount % 2;
        this.requestCount++;
        if (mod == 0) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                                                            .entity(new AuthErrorResponse(AuthErrorResponse.INVALID_GRANT,
                                                                                          "Credentials are invalid, "
                                                                                          + "expired or revoked."))
                                                            .build());
        }

        return "Hello world!";
    }

    @Override
    public TestModelWithFields testModelWithFields() {
        return new TestModelWithFields(1, "name");
    }
}
