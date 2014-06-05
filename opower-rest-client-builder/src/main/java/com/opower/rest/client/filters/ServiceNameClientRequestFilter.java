package com.opower.rest.client.filters;

import com.google.common.base.Preconditions;
import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link ClientRequestFilter} for adding the service name to the request header.
 * 
 * @author matthew.greenfield@opower.com
 */
public class ServiceNameClientRequestFilter implements ClientRequestFilter {
    public static final String X_SERVICE_NAME = "X-Service-Name";

    private final String serviceName;

    /**
     * Constructs a {@link ServiceNameClientRequestFilter} with a service name.
     *
     * @param serviceName the service name to use.
     */
    public ServiceNameClientRequestFilter(String serviceName) {
        this.serviceName = Preconditions.checkNotNull(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filter(ClientRequest request) {
        checkNotNull(request);

        request.header(X_SERVICE_NAME, this.serviceName);
    }
}
