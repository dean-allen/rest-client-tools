package com.opower.rest;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response object for exceptions from server.
 *
 * @author sheena.carswell
 */
public class ErrorResponse {
    public static final ErrorDetails EMPTY_DETAILS = new ErrorDetails(0, null, null);
    private ErrorDetails error;

    /**
     * Default constructor for jackson deserialization.
     */
    public ErrorResponse() {
        this.error = EMPTY_DETAILS;
    }

    /**
     * Constructor.
     * @param httpStatus the http status code of the response.
     * @param serviceErrorCode a machine-readable error code, specific to the service.  The service's API documentation
     *                         should define a list of all possible error codes.
     * @param details human-readable details of the error.
     */
    public ErrorResponse(int httpStatus, String serviceErrorCode, String details) {
        this.error = new ErrorDetails(httpStatus, serviceErrorCode, details);
    }

    public ErrorDetails getError() {
        return this.error;
    }

    /**
     * Error holds the details of the exception.
     */
    public static final class ErrorDetails {
        private final int httpStatus;
        private final String serviceErrorCode;
        private final String details;

        @JsonCreator
        private ErrorDetails(@JsonProperty("httpStatus") int httpStatus,
                             @JsonProperty("serviceErrorCode") String serviceErrorCode,
                             @JsonProperty("details") String details) {
            this.httpStatus = httpStatus;
            this.serviceErrorCode = serviceErrorCode;
            this.details = details;
        }

        public int getHttpStatus() {
            return this.httpStatus;
        }

        public String getServiceErrorCode() {
            return this.serviceErrorCode;
        }

        public String getDetails() {
            return this.details;
        }
    }

}