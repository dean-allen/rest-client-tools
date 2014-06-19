package com.opower.rest.client.generator.core;

import java.net.URI;

/**
 * Implementations of this interface are responsible for providing the base URI for all 
 * requests that the generated client proxies will make. In a sense, this is how you point
 * the ClientBuilders at their intended urls.
 * @author chris.phillips
 */
public interface UriProvider {

    /**
     * Build a base URI for the client proxies to use when making requests.
     * @return The base URI for the REST requests that the clients will make. Should not return null.
     */
    URI getUri();
}
