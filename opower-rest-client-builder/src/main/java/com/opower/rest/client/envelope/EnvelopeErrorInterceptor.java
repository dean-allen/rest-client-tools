package com.opower.rest.client.envelope;

import com.opower.rest.ErrorResponse;
import com.opower.rest.client.ExceptionMapperInterceptor;
import com.opower.rest.client.generator.core.ClientResponse;
import com.opower.rest.client.generator.util.GenericType;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attempts to unwrap an error from a service that send responses in a JSONEnvelope. Otherwise it delegates to the {@link
 * com.opower.rest.client.ExceptionMapperInterceptor}.
 *
 * @author ben.siemon
 * @since 1.1.1
 */
public class EnvelopeErrorInterceptor extends ExceptionMapperInterceptor {
    /**
     * Top-level key for error section.
     */
    public static final String ERROR_KEY = "error";

    /**
     * Nested key for HTTP error status.
     */
    public static final String ERROR_STATUS_KEY = "httpStatus";

    /**
     * Nested key for HTTP error message.
     */
    public static final String ERROR_MESSAGE_KEY = "errorMessage";

    /**
     * Nested key for HTTP error details.
     */
    public static final String ERROR_DETAILS_KEY = "details";

    private static final Logger LOG = LoggerFactory.getLogger(EnvelopeErrorInterceptor.class);

    @Override
    protected ErrorResponse.ErrorDetails getErrorDetails(ClientResponse response) {
        if (response.getHeaders().containsKey(EnvelopeJacksonProvider.HEADER_KEY)) {
            Map<String, Object> responseEnvelope = response.getEntity(
                    new GenericType<Map<String, Object>>() {
                    }
            );

            if (responseEnvelope != null && responseEnvelope.containsKey(ERROR_KEY)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorDetails = (Map<String, Object>) responseEnvelope.get(ERROR_KEY);
                String status = errorDetails.get(ERROR_STATUS_KEY).toString();
                String message = errorDetails.get(ERROR_MESSAGE_KEY).toString();
                String details = errorDetails.get(ERROR_DETAILS_KEY).toString();
                return new ErrorResponse(
                        Integer.parseInt(status),
                        message,
                        details).getError();
            } else {
                LOG.warn("An error was found from a %s enabled response but did not contain an \"error\" property.",
                        EnvelopeJacksonProvider.HEADER_KEY);
                return super.getErrorDetails(response);
            }
        } else {
            return super.getErrorDetails(response);
        }
    }
}
