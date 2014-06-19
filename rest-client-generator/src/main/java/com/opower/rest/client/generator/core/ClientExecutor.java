package com.opower.rest.client.generator.core;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientExecutor {

    void processFilters(ClientRequest request);
    ClientResponse execute(ClientRequest request) throws Exception;

    void close() throws Exception;
}
