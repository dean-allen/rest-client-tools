package com.opower.rest.test;

import com.opower.rest.ResourceMetadata;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Resource interface for testing.
 * 
 * @author matthew.greenfield
 */
@Path("/example")
@Produces(MediaType.APPLICATION_JSON)
@ResourceMetadata(serviceName = "example", serviceVersion = 1)
public interface ExampleResource {

    /**
     * Throws an unauthorized request exception on even number of requests.
     *
     * @return string
     */
    @GET
    String unauthorizedOnEvenRequests();
}
