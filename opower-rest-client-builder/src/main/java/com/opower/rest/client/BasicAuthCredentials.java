package com.opower.rest.client;

import com.opower.auth.resources.oauth2.AuthenticationScheme;
import org.eclipse.jetty.util.B64Code;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates the Authorization HTTP header for the Basic com.opower.auth-scheme.
 *
 * @author matthew.greenfield@opower.com
 */
public class BasicAuthCredentials implements AuthorizationCredentials {
    static final String CREDENTIALS_FMT = "%s %s";

    private final String username;
    private final String password;

    /**
     * Constructs {@link BasicAuthCredentials}.
     *
     * @param username the username.
     * @param password the password.
     */
    public BasicAuthCredentials(String username, String password) {
        this.username = checkNotNull(username);
        this.password = checkNotNull(password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHttpHeaderValue() {
        return String.format(CREDENTIALS_FMT, 
                             AuthenticationScheme.BASIC.getScheme(),
                             B64Code.encode(this.username + ":" + this.password));
    }
}
