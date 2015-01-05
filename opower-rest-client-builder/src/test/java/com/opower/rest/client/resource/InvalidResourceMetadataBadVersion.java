package com.opower.rest.client.resource;

import com.opower.rest.ResourceMetadata;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * This is for testing validation of the serviceName.
 * @author chris.phillips
 */
@Path("/test")
@ResourceMetadata(serviceName = "test", serviceVersion = 0)
public interface InvalidResourceMetadataBadVersion {

    /**
     * Dummy test method.
     * @return nothing
     */
    @GET
    String test();
}
