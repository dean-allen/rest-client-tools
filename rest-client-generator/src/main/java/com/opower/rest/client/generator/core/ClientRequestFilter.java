package com.opower.rest.client.generator.core;

/**
 * ClientRequestFilters allow manipulation of the http request before it is sent. This could mean
 * adding a special header or parameter etc. The filters are executed in order.
 * @author chris.phillips
 */
public interface ClientRequestFilter {

    /**
     * Filter method called before a request has been dispatched to a client transport layer.
     *
     * @param request the http request.
     */
    void filter(ClientRequest request);
}
