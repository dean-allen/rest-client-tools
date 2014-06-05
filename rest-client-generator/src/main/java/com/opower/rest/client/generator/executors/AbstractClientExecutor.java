package com.opower.rest.client.generator.executors;

import com.opower.rest.client.generator.core.ClientExecutor;
import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author chris.phillips
 */
public abstract class AbstractClientExecutor implements ClientExecutor {

    private final List<ClientRequestFilter> requestFilters;


    protected AbstractClientExecutor(List<ClientRequestFilter> requestFilters) {
        this.requestFilters = checkNotNull(requestFilters);
    }

    @Override
    public void processFilters(ClientRequest request) {
        for(ClientRequestFilter filter : requestFilters) {
            filter.filter(request);
        }
    }

}
