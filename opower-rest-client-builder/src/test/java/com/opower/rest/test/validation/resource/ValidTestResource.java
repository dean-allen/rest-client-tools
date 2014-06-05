package com.opower.rest.test.validation.resource;

import com.google.common.base.Optional;
import com.opower.rest.test.validation.model.ValidModel;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.joda.time.LocalDate;

/**
 * Test class to exercise validation logic of ResourceClass.
 * @author chris.phillips
 */
public interface ValidTestResource {

    /**
     * Dummy method annotated with a variety of jax-rs annotations so that validation will succeed.
     * @param work dummy param
     * @param otherParam dummy param
     * @return dummy return value.
     */
    @GET
    @Path("path")
    ValidModel doWork(@PathParam("test") LocalDate work, @MatrixParam("test") Optional<String> otherParam);
}