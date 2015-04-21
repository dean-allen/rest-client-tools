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
    public static final String ERROR_HTTP_STATUS_KEY = "httpStatus";
    public static final String ERROR_STATUS_KEY = "status";

    /**
     * Nested key for HTTP error message.
     */
    public static final String ERROR_ERROR_MESSAGE_KEY = "errorMessage";
    public static final String ERROR_MESSAGE_KEY = "message";

    /**
     * Nested key for error details.
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
                String status = find(errorDetails, ERROR_HTTP_STATUS_KEY, ERROR_STATUS_KEY);
                String errorCode = find(errorDetails, ERROR_ERROR_MESSAGE_KEY, ERROR_MESSAGE_KEY);
                String details = find(errorDetails, ERROR_DETAILS_KEY);
                return new ErrorResponse(
                        Integer.parseInt(status),
                        errorCode,
                        details).getError();
            } else {
                LOG.warn("An error was found from a {} enabled response but did not contain an \"error\" property.",
                        EnvelopeJacksonProvider.HEADER_KEY);
                return super.getErrorDetails(response);
            }
        } else {
            return super.getErrorDetails(response);
        }
    }

    /**
     * Find any value from a list of possible keys or return null if none found.
     *
     * @param error The error map holding the error values
     * @param keys The alternative keys to look for
     * @return A string value for any of the matching keys
     */
    private String find(Map<String, Object> error, String... keys) {
        for (String key : keys) {
            if (error.containsKey(key)) {
                return error.get(key).toString();
            }
        }
        return null;
    }
}
