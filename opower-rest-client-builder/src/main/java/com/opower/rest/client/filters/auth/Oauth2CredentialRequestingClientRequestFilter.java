package com.opower.rest.client.filters.auth;

import com.google.common.net.HttpHeaders;
import com.opower.rest.client.AuthorizationCredentials;
import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link ClientRequestFilter} that fetches {@link AuthorizationCredentials} and uses them for the 
 * Authorization header.
 *
 * @author matthew.greenfield@opower.com
 */
public class Oauth2CredentialRequestingClientRequestFilter implements ClientRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2CredentialRequestingClientRequestFilter.class);

    private final Oauth2AccessTokenRequester credentialRequester;

    /**
     * Constructs a {@link Oauth2CredentialRequestingClientRequestFilter}.
     *
     * @param credentialRequester the credential flow to use.
     */
    public Oauth2CredentialRequestingClientRequestFilter(Oauth2AccessTokenRequester credentialRequester) {
        this.credentialRequester = checkNotNull(credentialRequester);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void filter(ClientRequest request) {
        checkNotNull(request);

        AuthorizationCredentials authorizationCredentials = this.credentialRequester.getAccessToken();

        LOG.debug("Adding OAuth2 Bearer Authorization header to request");
        request.header(HttpHeaders.AUTHORIZATION, authorizationCredentials.getHttpHeaderValue());

    }
}
