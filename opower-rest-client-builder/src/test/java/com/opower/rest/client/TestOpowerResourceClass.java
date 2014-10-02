package com.opower.rest.client;

import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.rest.test.validation.resource.ValidTestResource;
import javax.ws.rs.GET;
import org.eclipse.jetty.server.Server;
import org.junit.Test;


/**
 * Test for OpowerResourceClass.
 * @author chris.phillips
 */
public class TestOpowerResourceClass {

    /**
     * Make sure the auth-service interface is valid.
     */
    @Test
    public void authResourceIsValid() {
        new OpowerResourceInterface(AccessTokenResource.class);
    }

    /**
     * This ValidTestResource has the various annotations etc that will exercise the validation
     * logic.
     */
    @Test
    public void testResourceInterfaceIsValid() {
        new OpowerResourceInterface(ValidTestResource.class, "com.opower.rest.test.validation.model", "opower.test.package");
    }

    /**
     * Make sure an Invalid interface throws an exception. In this case, The return type of Server
     * is not allowed.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testExternalPackageNotSpecifiedThrows() {
        new OpowerResourceInterface(InvalidResource.class);
    }

    /**
     * Make sure null for supporting packages throws.
     */
    @Test(expected = NullPointerException.class)
    public void nullSupportingPackagesThrows() {
        new OpowerResourceInterface<>(ValidTestResource.class, null);

    }

    /**
     * Make sure multiple nulls for supporting packages throws.
     */
    @Test(expected = NullPointerException.class)
    public void multipleNullSupportingPackagesThrows() {
        new OpowerResourceInterface<>(ValidTestResource.class, null, null);

    }

    private interface InvalidResource {
        @GET
        Server getServer();
    }
}
