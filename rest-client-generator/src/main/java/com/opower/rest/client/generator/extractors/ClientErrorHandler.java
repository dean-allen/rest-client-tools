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
