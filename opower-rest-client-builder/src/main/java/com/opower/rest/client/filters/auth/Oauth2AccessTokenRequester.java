package com.opower.rest.client.filters.auth;


import com.opower.auth.model.oauth2.GrantType;
import com.opower.auth.oauth2.BearerToken;
import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.auth.resources.oauth2.AccessTokenResponse;
import com.opower.rest.client.BearerAuthCredentials;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static com.google.common.base.Preconditions.checkState;

/**
 * Access token requester for requesting OAuth2 from the authorization service.
 * <p/>
 * Note: This class is intended for multi-threaded use to prevent a high number of requests from retrieving an access token
 * when only one request is needed to update it. {@link #getAccessToken()} is {@code synchronized} around {@code this} instance.
 *
 * @author matthew.greenfield@opower.com
 */
public class Oauth2AccessTokenRequester {
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2AccessTokenRequester.class);
    private static final String INVALID_TOKEN_TTL_REFRESH_MESSAGE = "tokenTtlRefresh must be greater than or equal to zero";

    private final AccessTokenResource accessTokenClient;
    private final int tokenTtlRefresh;

    private volatile BearerAuthCredentials bearerCredentials;
    private volatile Long expiresTimeInMilliSeconds;

    /**
     * Constructs an {@link Oauth2AccessTokenRequester}.
     *
     * @param accessTokenResource The auth service client to use
     * @param tokenTtlRefresh     the duration, in seconds, until the token expires for which to try to refresh the token.
     */
    public Oauth2AccessTokenRequester(AccessTokenResource accessTokenResource,
                                      int tokenTtlRefresh) {
        checkState(tokenTtlRefresh >= 0, INVALID_TOKEN_TTL_REFRESH_MESSAGE);
        this.accessTokenClient = accessTokenResource;
        this.tokenTtlRefresh = tokenTtlRefresh;
    }

    /**
     * Get the OAuth2 access token.
     * <p/>
     * Only try to regenerate a token if it expires within a second. Because generating a token that hasn't expired
     * returns the currently active token, we don't want to try to get a new token until the last possible second to
     * reduce the number of times we actually have to hit the auth service.
     *
     * @return the {@link BearerAuthCredentials}.
     */
    public BearerAuthCredentials getAccessToken() {
        if (shouldRefreshAccessToken()) {
            // synchronizing to prevent duplicate requests to the authorization server from being made. All threads will share
            // the same access token.
            synchronized (this) {
                if (shouldRefreshAccessToken()) {
                    LOG.debug("Attempting to refresh access token");
                    refreshAccessToken();
                }
            }
        }

        return this.bearerCredentials;
    }

    /**
     * Determine whether or not an access token should be refreshed.
     *
     * @return true if the access token should be refreshed, false otherwise.
     */
    private boolean shouldRefreshAccessToken() {
        Long expiresInSeconds = getExpiresInSeconds();
        return this.bearerCredentials == null || expiresInSeconds == null || expiresInSeconds < this.tokenTtlRefresh;
    }

    /**
     * The time until the access token expires, in seconds.
     *
     * @return the seconds until the access token expires. If negative, the credential has already expired.
     */
    private Long getExpiresInSeconds() {
        return this.expiresTimeInMilliSeconds == null ? null : TimeUnit.MILLISECONDS.toSeconds(
                this.expiresTimeInMilliSeconds - System.currentTimeMillis());
    }

    /**
     * Sets the time at which the access token expires.
     *
     * @param expiresInSeconds the length of time until the access token expires, in seconds.
     */
    private void setExpiresTimeInSeconds(long expiresInSeconds) {
        this.expiresTimeInMilliSeconds = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresInSeconds);
    }

    /**
     * Attempts to refresh the OAuth2 access token.
     */
    private void refreshAccessToken() {
        AccessTokenResponse response = this.accessTokenClient.tokenEndpoint(null, GrantType.CLIENT_CREDENTIALS.alias());
        LOG.debug("Access token for client [%s] refreshed. Expires in %d",
                  response.getClientId(),
                  response.getExpiresIn());

        setExpiresTimeInSeconds(response.getExpiresIn());
        this.bearerCredentials = new BearerAuthCredentials(new BearerToken(response.getAccessToken()));
    }
}
