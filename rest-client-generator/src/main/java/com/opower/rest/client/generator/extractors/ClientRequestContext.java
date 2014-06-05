package com.opower.rest.client.generator.extractors;

import com.opower.rest.client.generator.core.BaseClientResponse;
import com.opower.rest.client.generator.core.ClientRequest;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClientRequestContext {
    private final ClientRequest request;
    private final BaseClientResponse<?> clientResponse;
    private final ClientErrorHandler errorHandler;

    public ClientRequestContext(ClientRequest request, BaseClientResponse<?> clientResponse,
                                ClientErrorHandler errorHandler) {
        this.request = checkNotNull(request);
        this.clientResponse = checkNotNull(clientResponse);
        this.errorHandler = checkNotNull(errorHandler);
    }

    public ClientRequest getRequest() {
        return request;
    }

    public BaseClientResponse getClientResponse() {
        return clientResponse;
    }

    public ClientErrorHandler getErrorHandler() {
        return errorHandler;
    }

}
