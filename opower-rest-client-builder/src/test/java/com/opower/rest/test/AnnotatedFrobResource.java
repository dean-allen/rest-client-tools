package com.opower.rest.test;

import com.opower.rest.ResourceMetadata;
import com.opower.rest.test.resource.FrobResource;
import javax.ws.rs.Path;

/**
 * This allows us to test the ResourceMetadata annotation in the integration tests.
 * @author chris.phillips
 */
@Path("/frob")
@ResourceMetadata(serviceName = "frob", serviceVersion = 1)
public interface AnnotatedFrobResource extends FrobResource {
}
