package com.opower.rest.client;

/**
 * Interface for producing the value for the Authorization HTTP header.
 *
 * @author benson.fung@opower.com
 *
 */
public interface AuthorizationCredentials {

    /**
     * Get a fully formatted Authorization scheme specific credentials line used in the Authorization HTTP header.
     *
     * @return the Authorization HTTP header value to use in requests.
     */
    String getHttpHeaderValue();
}