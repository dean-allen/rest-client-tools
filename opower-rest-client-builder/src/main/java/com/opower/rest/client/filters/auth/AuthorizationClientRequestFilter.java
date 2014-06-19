package com.opower.rest.client.filters.auth;

import com.google.common.net.HttpHeaders;
import com.opower.rest.client.AuthorizationCredentials;
import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adds an Authorization header to a request.
 *
 * @author matthew.greenfield@opower.com
 */
public class AuthorizationClientRequestFilter implements ClientRequestFilter {
    private final AuthorizationCredentials authorizationCredentials;

    /**
     * Creates an {AuthorizationClientRequestFilter} with credentials.
     *
     * @param authorizationCredentials the {@link AuthorizationCredentials}.
     */
    public AuthorizationClientRequestFilter(AuthorizationCredentials authorizationCredentials) {
        this.authorizationCredentials = checkNotNull(authorizationCredentials);
    }

    @Override
    public void filter(ClientRequest request) {
        checkNotNull(request);

        request.header(HttpHeaders.AUTHORIZATION, this.authorizationCredentials.getHttpHeaderValue());

    }
}
