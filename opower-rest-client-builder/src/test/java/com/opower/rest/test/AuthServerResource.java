package com.opower.rest.test;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.opower.auth.model.oauth2.GrantType;
import com.opower.auth.oauth2.Scope;
import com.opower.auth.oauth2.TokenType;
import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.auth.resources.oauth2.AccessTokenResponse;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.util.B64Code;
import org.joda.time.Instant;


/**
 * Dummy implementation of the authserver resource. Used for testing the authentication interaction of
 * the various clients.
 * @author chris.phillips
 */
public class AuthServerResource implements AccessTokenResource {
    static final String VALIDATED_TOKEN = "urn:opower.com:oauth2:validated_token";

    private static final Set<Scope> SCOPES = ImmutableSet.of();
    private static final Cache<String, AccessTokenResponse> TOKENS = CacheBuilder.newBuilder().build();

    @Context
    HttpServletRequest request;

    @Override
    public AccessTokenResponse tokenEndpoint(final String accessToken, final String grantTypeAlias) {
        try {
            GrantType grantType = grantTypeAlias == null ? null : GrantType.findByAlias(grantTypeAlias.trim().toLowerCase());

            AccessTokenResponse response;
            if (GrantType.CLIENT_CREDENTIALS == grantType) {
                final String[] creds = B64Code.decode(this.request.getHeader("Authorization"), "UTF-8").split(":");
                final String newToken = UUID.randomUUID().toString();
                response = TOKENS.get(newToken, new Callable<AccessTokenResponse>() {
                    @Override
                    public AccessTokenResponse call() throws Exception {
                        Instant expiresAt = Instant.now().plus(TimeUnit.DAYS.toMillis(1));
                        return new AccessTokenResponse(creds[0], newToken, TokenType.BEARER.name(), SCOPES,computeExpiresInSeconds(
                                expiresAt));
                    }
                });
            } else if (GrantType.OPOWER_VALIDATE_BEARER == grantType) {
                response = TOKENS.getIfPresent(accessToken);
                if (response == null) {
                    throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                                                              .entity(new AuthErrorResponse(AuthErrorResponse.INVALID_GRANT,
                                                                                            "Credentials are invalid, " 
                                                                                            + "expired or revoked."))
                                                              .build());
                } else {
                    response = new AccessTokenResponse(response.getClientId(),
                                                       response.getAccessToken(),
                                                       VALIDATED_TOKEN,
                                                       SCOPES,
                                                       response.getExpiresIn());
                    TOKENS.put(accessToken, response);
                }

            } else {
                throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                                                          .entity(new AuthErrorResponse(AuthErrorResponse
                                                                                                .UNSUPPORTED_GRANT_TYPE,
                                                                                        "Unsupported grant type."))
                                                          .build());
            }

            return response;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Compute the number of seconds until a token expires. This value will never be less than 0.
     *
     * @param expiresAt the {@link org.joda.time.Instant} at which the token expires.
     * @return the number of seconds until the expiresAt is reached.
     */
    static long computeExpiresInSeconds(Instant expiresAt) {
        Instant now = Instant.now();

        return TimeUnit.MILLISECONDS.toSeconds(expiresAt.isBefore(now) ? 0 : expiresAt.getMillis() - now.getMillis());
    }

}
