package com.opower.rest.client.resource;

import com.opower.rest.ResourceMetadata;
import com.opower.rest.client.model.TestModel;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * This is for testing validation of the modelPackages.
 * @author chris.phillips
 */
@ResourceMetadata(
        serviceName = "test",
        serviceVersion = 1,
        modelPackages = "com.opower.rest.client.model"
)
@Path("/test")
public interface ValidResourceMetadata {

    /**
     * Dummy test method.
     * @return nothing
     */
    @GET
    TestModel test();
}
