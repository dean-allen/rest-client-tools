package com.opower.rest.client.filters;

import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Filter that adds a unique request_id header to each request.
 * @author chris.phillips
 */
public class RequestIdFilter implements ClientRequestFilter {
    public static final String REQUEST_ID_PARAM = "request_id";

    @Override
    public void filter(ClientRequest request) {

        checkNotNull(request);

        if (request.getHeaders().get(REQUEST_ID_PARAM) != null) {
            request.getHeaders().remove(REQUEST_ID_PARAM);
        }

        request.header(REQUEST_ID_PARAM, UUID.randomUUID().toString());

    }
}
