package com.opower.rest.client.generator.marshallers;


import com.opower.rest.client.generator.core.ClientRequest;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Marshaller {
    void build(ClientRequest request, Object target);
}