package com.opower.rest.test;

import com.opower.rest.ResourceMetadata;
import com.opower.rest.client.model.TestModelWithFields;

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
    @Path("unauthorizedOnEvenRequests")
    String unauthorizedOnEvenRequests();

    /**
     * Simply returns a populated TestModelWithFields instance.
     *
     * @return TestModelWithFields instance with values id:1 and name:"name"
     */
    @GET
    @Path("testModelWithFields")
    TestModelWithFields testModelWithFields();
}
