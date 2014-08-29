package com.opower.rest.client.generator.extractors;

import com.google.common.collect.Lists;
import com.opower.rest.client.generator.core.BaseClientResponse;
import com.opower.rest.client.generator.core.ClientErrorInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This class handles client errors (of course...).
 *
 * @author Solomon.Duskis
 * @author chris.phillips
 */

// TODO: expand this class for more robust, complicated error handling

public class ClientErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClientErrorHandler.class);
    private List<ClientErrorInterceptor> interceptors = Lists.newArrayList();

    public ClientErrorHandler(List<ClientErrorInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @SuppressWarnings("unchecked")
    public void clientErrorHandling(BaseClientResponse clientResponse, RuntimeException e) {
        // any of the ClientErrorInterceptors might throw a more appropriate exception. Or there might be a bug in one of the
        // ClientErrorInterceptors that throws a confusing Exception and the real source of the problem gets swallowed.
        // To avoid that, always log the original Exception here before allowing the ClientErrorInterceptors to proceed
        // If there are no ClientErrorInterceptors, then the original exception will be thrown and there's no need to log it
        if(!interceptors.isEmpty()) {
            LOG.error("Error while processing response", e);
        }
        for (ClientErrorInterceptor handler : interceptors) {
            try {
                // attempt to reset the stream in order to provide a fresh stream
                // to each ClientErrorInterceptor -- failing to reset the stream
                // could mean that an unusable stream will be passed to the
                // interceptor
                InputStream stream = clientResponse.getStreamFactory().getInputStream();
                if (stream != null) {
                    stream.reset();
                }
            } catch (IOException e1) {
                LOG.warn("problem while handling errors", e1);
            }
            handler.handle(clientResponse);
        }
        throw e;
    }
}
