package com.opower.rest.client;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.opower.rest.ErrorResponse;
import com.opower.rest.client.generator.core.BaseClientResponse;
import com.opower.rest.client.generator.core.ClientErrorInterceptor;
import com.opower.rest.client.generator.core.ClientResponse;
import com.opower.rest.exception.BadRequestException;
import com.opower.rest.exception.ErrorResponseException;
import com.opower.rest.exception.ForbiddenException;
import com.opower.rest.exception.GoneException;
import com.opower.rest.exception.InternalServerErrorException;
import com.opower.rest.exception.NotFoundException;
import com.opower.rest.exception.ServiceUnavailableException;
import com.opower.rest.exception.UnauthorizedRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_GONE;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * ClientErrorInterceptor implementation that translates HTTP error responses into Archmage exceptions.
 * Note that this will only be called for HTTP status codes greater than 399.
 *
 * @author derrick.schneider@opower.com
 * @since 0.20
 *
 */
public class ExceptionMapperInterceptor implements ClientErrorInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionMapperInterceptor.class);

    /**
     * Throw an ErrorResponseException based on the response status code.
     *
     * @param response the ClientResponse object to be handled.
     * @throws ErrorResponseException for the given response's status code.
     */
    public void handle(ClientResponse response) {
        ErrorResponse.ErrorDetails errorDetails = getErrorDetails(response);

        int status = response.getStatus();
        switch (status) {
            case SC_BAD_REQUEST:
                throw new BadRequestException(errorDetails.getServiceErrorCode(), errorDetails.getDetails());
            case SC_FORBIDDEN:
                throw new ForbiddenException(errorDetails.getServiceErrorCode(), errorDetails.getDetails());
            case SC_NOT_FOUND:
                throw new NotFoundException(errorDetails.getServiceErrorCode(), errorDetails.getDetails());
            case SC_GONE:
                throw new GoneException(errorDetails.getServiceErrorCode(), errorDetails.getDetails());
            case SC_INTERNAL_SERVER_ERROR:
                throw new InternalServerErrorException(errorDetails.getServiceErrorCode(), errorDetails.getDetails());
            case SC_SERVICE_UNAVAILABLE:
                throw new ServiceUnavailableException(errorDetails.getServiceErrorCode(), errorDetails.getDetails());
            case SC_UNAUTHORIZED:
                throw new UnauthorizedRequestException(errorDetails.getServiceErrorCode(), errorDetails.getDetails());
            default:
                throw new ErrorResponseException(
                        status, errorDetails.getServiceErrorCode(), errorDetails.getDetails());
        }
    }

    /**
     * Extracts the error details from the given {@link com.opower.rest.client.generator.core.ClientResponse}.
     * @param response the ClientResponse object to be handled.
     * @return a possibly null instance of ErrorDetails containing information about the request failure.
     */
    protected ErrorResponse.ErrorDetails getErrorDetails(ClientResponse response) {
        ErrorResponse errorResponse;
        try {
            errorResponse = response.getEntity(ErrorResponse.class);
            if (errorResponse.getError().getDetails() == null) {
                return errorWithResponseBody(response);
            }
        } catch (NullPointerException e) {
            LOG.debug("Server has not set exception response %s", e);
            return ErrorResponse.EMPTY_DETAILS;
        } catch (RuntimeException e) {
            LOG.error("Unable to parse ExceptionResponse", e);
            return errorWithResponseBody(response);
        }

        ErrorResponse.ErrorDetails errorDetails = errorResponse.getError();
        if (errorDetails == null) {
            LOG.error("No details in parsed ExceptionResponse");
        }

        return errorDetails;
    }

    /**
     * Read the response body into a String.
     *
     * @param response the response to read
     * @return the response body as a String
     */
    public static String fetchResponseBody(ClientResponse response) {
        try {
            if (!response.resetStream()) {
                return null;
            }

            InputStream stream;
            if (response instanceof BaseClientResponse) {
                BaseClientResponse baseResponse = (BaseClientResponse) response;
                BaseClientResponse.BaseClientResponseStreamFactory streamFactory = baseResponse.getStreamFactory();
                stream = streamFactory.getInputStream();
            } else {
                stream = response.getEntity(InputStream.class);
            }

            InputStreamReader reader = new InputStreamReader(stream);
            String bodyString = CharStreams.toString(reader);
            Closeables.close(reader, true);

            return bodyString;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Creates an {@link com.opower.rest.ErrorResponse} from the given
     * {@link com.opower.rest.client.generator.core.ClientResponse} if found else returns the EMPTY_RESPONSE.
     *
     * @param response the ClientResponse object to be handled.
     * @return the error details found in the response or the EMPTY_DETAILS object if none found.
     */
    protected static ErrorResponse.ErrorDetails errorWithResponseBody(ClientResponse response) {
        String body = fetchResponseBody(response);
        if (body != null) {
            return new ErrorResponse(response.getStatus(), null, body).getError();
        }

        return ErrorResponse.EMPTY_DETAILS;
    }
}
