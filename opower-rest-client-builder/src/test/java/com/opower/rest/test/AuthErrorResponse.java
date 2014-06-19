package com.opower.rest.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.hibernate.validator.constraints.NotEmpty;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents the entity body in a Client Registration Error Response.
 * <p/>
 * <p>
 * See <a href="http://tools.ietf.org/html/draft-ietf-oauth-dyn-reg-16#section-5.2">
 * http://tools.ietf.org/html/draft-ietf-oauth-dyn-reg-16#section-5.2</a>.
 * </p>
 *
 * @author benson.fung@opower.com
 */
public class AuthErrorResponse {

    /**
     * <p><pre>
     * invalid_redirect_uri  The value of one or more "redirect_uris" is
     *     invalid.
     * </pre></p>
     */
    public static final String INVALID_REDIRECT_URI = "invalid_redirect_uri";

    /**
     * <p><pre>
     * invalid_client_metadata  The value of one of the client metadata
     *     fields is invalid and the server has rejected this request.  Note
     *     that an Authorization server MAY choose to substitute a valid
     *     value for any requested parameter of a client's metadata.
     * </pre></p>
     */
    public static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";

    /**
     * <p><pre>
     * invalid_software_statement  The software statement presented is
     *     invalid.
     * </pre></p>
     */
    public static final String INVALID_SOFTWARE_STATEMENT = "invalid_software_statement";

    /**
     * <p><pre>
     * unapproved_software_statement  The software statement presented is
     *     not approved for use with this authorization server.
     * </pre></p>
     */
    public static final String UNAPPROVED_SOFTWARE_STATEMENT = "unapproved_software_statement";

    /**
     * <p><pre>
     * invalid_client  Client authentication failed (e.g., unknown client, no client authentication included, or unsupported
     *     authentication method).  The authorization server MAY return an HTTP 401 (Unauthorized) status code to indicate
     *     which HTTP authentication schemes are supported.  If the client attempted to authenticate via the "Authorization"
     *     request header field, the authorization server MUST respond with an HTTP 401 (Unauthorized) status code and
     *     include the "WWW-Authenticate" response header field matching the authentication scheme used by the client.
     * </pre></p>
     */
    public static final String INVALID_CLIENT = "invalid_client";

    /**
     * <p><pre>
     * unsupported_grant_type  The authorization grant type is not supported by the authorization server.
     * </pre></p>
     */
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

    /**
     * <p><pre>unauthorized_client  The client is not authorized to request an authorization code using this method.</pre></p>
     */
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";

    /**
     * <p><pre>
     * invalid_grant  The provided authorization grant (e.g., authorization code, resource owner credentials) or refresh token
     *      is invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to
     *      another client.
     * </pre></p>
     */
    public static final String INVALID_GRANT = "invalid_grant";

    private static final String ERROR_PROPERTY = "error";
    private static final String ERROR_DESCRIPTION_PROPERTY = "errorDescription";

    private final String error;
    private final String errorDescription;

    /**
     * Construct a new Error Response.
     *
     * @param error            a Single ASCII error code string.
     * @param errorDescription a human-readable ASCII text description of the error used for debugging.
     */
    @JsonCreator
    public AuthErrorResponse(@NotEmpty @JsonProperty(ERROR_PROPERTY) String error,
                             @NotEmpty @JsonProperty(ERROR_DESCRIPTION_PROPERTY) String errorDescription) {
        this.error = checkNotBlank(error);
        this.errorDescription = checkNotBlank(errorDescription);
    }

    /**
     * Gets the error code.
     *
     * @return the error code.
     */
    @JsonProperty(ERROR_PROPERTY)
    public String getError() {
        return this.error;
    }

    /**
     * Gets the error description.
     *
     * @return the error description.
     */
    @JsonProperty(ERROR_DESCRIPTION_PROPERTY)
    public String getErrorDescription() {
        return this.errorDescription;
    }

    private String checkNotBlank(String error) {
        checkArgument(!Strings.isNullOrEmpty(error) && error.trim().length() > 0);
        return error;
    }

}
