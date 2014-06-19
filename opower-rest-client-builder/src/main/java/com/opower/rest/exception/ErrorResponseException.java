package com.opower.rest.exception;

import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 *  Base class for exceptions triggered by errors returned via HTTP.
 *
 *  This class contain changes that are not backward compatible between archmage-0.1.4 and archmage-0.1.5
 *  errorCode and errorMessage were renamed statusCode and statusText, to more clearly differentiate from
 *  service-specific error code and message.
 *
 *  @author derrick.schneider@opower.com
 *  @since 0.20
 */
public class ErrorResponseException extends RuntimeException {


    /** <tt>400 Bad Request</tt> (HTTP/1.1 - RFC 2616). */
    public static final int SC_BAD_REQUEST = 400;
    /** <tt>403 Forbidden</tt> (HTTP/1.0 - RFC 1945). */
    public static final int SC_FORBIDDEN = 403;
    /** <tt>410 Gone</tt> (HTTP/1.1 - RFC 2616). */
    public static final int SC_GONE = 410;
    /** <tt>500 Server Error</tt> (HTTP/1.0 - RFC 1945). */
    public static final int SC_INTERNAL_SERVER_ERROR = 500;
    /** <tt>404 Not Found</tt> (HTTP/1.0 - RFC 1945). */
    public static final int SC_NOT_FOUND = 404;
    /** <tt>503 Service Unavailable</tt> (HTTP/1.0 - RFC 1945). */
    public static final int SC_SERVICE_UNAVAILABLE = 503;
    /** <tt>401 Unauthorized</tt> (HTTP/1.0 - RFC 1945). */
    public static final int SC_UNAUTHORIZED = 401;

    private static final long serialVersionUID = -8171566598445748748L;

    // http components no longer seems to have a static getStatusText method
    // so build a map of status codes to response phrases
    private static final Map<Integer,String> CODE_TO_RESPONSE_PHRASE = ImmutableMap.<Integer,String>builder()
            .put(SC_BAD_REQUEST,"Bad Request")
            .put(SC_FORBIDDEN,"Forbidden")
            .put(SC_NOT_FOUND,"Not Found")
            .put(SC_GONE,"Gone")
            .put(SC_INTERNAL_SERVER_ERROR,"Internal Server Error")
            .put(SC_SERVICE_UNAVAILABLE,"Service Unavailable")
            .build();
    private static final String UNKNOWN_STATUS_MESSAGE = "Unknown";

    /**
     * The http status code.
     */
    private final int statusCode;
    /**
     * A service-specific error code.  Note that depending on the service, this field may be unused ie. null.
     */
    private final String serviceErrorCode;


    /**
     * Constructor that takes just a status code.
     *
     * @param statusCode the HTTP Status code
     */
    public ErrorResponseException(int statusCode) {
        this(statusCode, null, null);
    }

    /**
     * Basic constructor that takes an http status code, error code and error message.
     *
     * @param statusCode the HTTP status code that triggered this Exception.
     * @param serviceErrorCode the service-specific error code.
     * @param message the error message.
     */
    public ErrorResponseException(int statusCode, String serviceErrorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.serviceErrorCode = serviceErrorCode;
    }

    /**
     * Get the HTTP status code that triggered this exception.
     * @return the HTTP status code that triggered this exception.
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Get the error message associated with this exception.
     * @return the error message associated with this exception.
     */
    public String getStatusText() {
        return getStatusText(this.statusCode);
    }

    /**
     * Given an HTTP Status Code, return the reason phrase.
     *
     * @param statusCode HTTP Status code
     * @return the reason phrase for the code, or "Unknown"
     */
    protected static String getStatusText(int statusCode) {
        // note that this boxes the int (potentially twice!) to access the map
        // but the hope is that error responses are relatively uncommon, so this
        // shouldn't cause a lot of object churn
        if (CODE_TO_RESPONSE_PHRASE.containsKey(statusCode)) {
            return CODE_TO_RESPONSE_PHRASE.get(statusCode);
        } else {
            return UNKNOWN_STATUS_MESSAGE;
        }
    }

    /**
     * Return a server-specific error code for the exception.
     * @return the error code, which may be null.
     */
    public String getServiceErrorCode() {
        return this.serviceErrorCode;
    }

    /**
     * Add fields from ErrorResponseException for easier debugging.
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        ToStringHelper toStringHelper = Objects.toStringHelper(this);
        toStringHelper.add("statusCode", getStatusCode());
        toStringHelper.add("statusText", getStatusText());
        toStringHelper.add("serviceErrorCode", getServiceErrorCode());
        toStringHelper.add("message", getMessage());
        return toStringHelper.toString();
    }

}