package com.opower.rest.client.generator.core;


/**
 * {@link ClientErrorInterceptor} provides a hook into the rest
 * {@link com.opower.rest.client.generator.core.ClientResponse} request lifecycle. If a Client Proxy method is called,
 * resulting in a client exception, and the rest return type is not
 * {@link javax.ws.rs.core.Response} or {@link com.opower.rest.client.generator.core.ClientResponse}, registered interceptors will be
 * given a chance to process the response manually, or throw a new exception. If
 * all interceptors successfully return, RestEasy will re-throw the original
 * encountered exception.
 *
 * @author <a href="mailto:lincoln@ocpsoft.com">Lincoln Baxter, III</a>
 */
public interface ClientErrorInterceptor {
    /**
     * Attempt to handle the current {@link com.opower.rest.client.generator.core.ClientResponse}. If this method
     * returns successfully, the next registered
     * {@link ClientErrorInterceptor} will attempt to handle the
     * {@link com.opower.rest.client.generator.core.ClientResponse}. If this method throws an exception, no further
     * interceptors will be processed.
     *
     * @throws RuntimeException RestEasy will abort request processing if any exception is
     *                          thrown from this method.
     */
    void handle(ClientResponse<?> response) throws RuntimeException;
}
