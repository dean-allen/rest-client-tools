package com.opower.rest.client.generator.marshallers;


import com.opower.rest.client.generator.core.ClientRequest;

/**
 * Marshaller that doesn't do anything with the target. Useful for @Context parameters which are server-generated.
 *
 * @author Stephane Epardaud
 */
public class NOOPMarshaller implements Marshaller {

    public void build(ClientRequest request, Object target) {
        // do nothing at all
    }

}
