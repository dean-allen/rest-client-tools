package com.opower.rest.test.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The JAX-RS Resource interface we will use to exercise the various JAX-RS implementations with
 * the various client configurations.
 * @author chris.phillips
 */
@Path("/frob")
@Produces(MediaType.APPLICATION_JSON)
public interface FrobResource {

    /**
     * This method makes sure that @FormParam and POST methods work.
     * @param frobId frobId to use
     * @param name a param to test the @FormParam with
     * @return a Frob
     */
    @POST
    @Path("{frobId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Frob updateFrob(@PathParam("frobId")String frobId, @FormParam("name") String name);

    /**
     * This method makes sure that GET with @PathParam works.
     * @param frobId the frob id to use
     * @return the Frob you were looking for
     */
    @GET
    @Path("{frobId}")
    Frob findFrob(@PathParam("frobId") String frobId);


    /**
     * This method tests @PUT methods and returning
     * Response objects from methods.
     * @param frob the frob to create
     * @return the Response object.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response createFrob(Frob frob);

}
