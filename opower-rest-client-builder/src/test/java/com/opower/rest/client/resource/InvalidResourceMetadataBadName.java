package com.opower.rest.client.resource;

import com.opower.rest.ResourceMetadata;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * This is for testing validation of the serviceName.
 * @author chris.phillips
 */
@Path("/test")
@ResourceMetadata(serviceName = "  ", serviceVersion = 1)
public interface InvalidResourceMetadataBadName {

    /**
     * Dummy test method.
     * @return nothing
     */
    @GET
    String test();
}
