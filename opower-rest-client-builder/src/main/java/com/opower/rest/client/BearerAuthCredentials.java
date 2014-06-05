package com.opower.rest.client;


import com.opower.auth.oauth2.BearerToken;
import com.opower.auth.resources.oauth2.AuthenticationScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates the Authorization HTTP header for the Bearer com.opower.auth-scheme.
 *
 * @author benson.fung@opower.com
 *
 */
public class BearerAuthCredentials implements AuthorizationCredentials {

    private static final String CREDENTIALS_FMT = "%s %s";

    private final BearerToken bearerToken;

    /**
     * Construct the Bearer com.opower.auth credentials with the given bearerToken.
     *
     * @param bearerToken the access token used with the Bearer com.opower.auth-scheme.
     */
    public BearerAuthCredentials(BearerToken bearerToken) {
        super();
        this.bearerToken = checkNotNull(bearerToken);
    }

    @Override
    public String getHttpHeaderValue() {
        return String.format(CREDENTIALS_FMT, AuthenticationScheme.BEARER.getScheme(), this.bearerToken.getValue());
    }
}
