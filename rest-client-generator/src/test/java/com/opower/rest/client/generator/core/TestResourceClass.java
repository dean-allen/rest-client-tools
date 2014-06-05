package com.opower.rest.client.generator.core;

import org.junit.Test;

import javax.ws.rs.GET;

/**
 * Tests the validation logic in the ResourceClass.
 * @author chris.phillips
 */
public class TestResourceClass {

    @Test(expected = IllegalArgumentException.class)
    public void nonInterfaceFailsValidation() {
        new ResourceInterface<>(String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noJaxRsAnnotatedMethodsFails() {
        new ResourceInterface<>(NonJaxRsAnnotated.class);
    }

    @Test(expected = NullPointerException.class)
    public void nullClassFails() {
        new ResourceInterface<Object>(null);
    }

    @Test
    public void validResourceDoesNotFail() {
        new ResourceInterface<>(ValidResourceClass.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void almostValidResourceFails() {
        new ResourceInterface<>(AlmostValidResourceClass.class);
    }
    private interface NonJaxRsAnnotated {
        String testMethod();
    }

    private interface AlmostValidResourceClass {
        @GET
        String getId();
        void createUser();
    }

    private interface ValidResourceClass {
        @GET
        public String getId();
    }
}
