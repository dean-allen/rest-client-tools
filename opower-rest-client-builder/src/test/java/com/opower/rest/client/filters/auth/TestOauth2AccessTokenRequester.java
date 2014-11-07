package com.opower.rest.client.filters.auth;

import com.google.common.collect.ImmutableSet;
import com.opower.auth.model.oauth2.GrantType;
import com.opower.auth.oauth2.Scope;
import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.auth.resources.oauth2.AccessTokenResponse;
import com.opower.rest.client.AuthorizationCredentials;
import com.opower.rest.client.BasicAuthCredentials;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link Oauth2AccessTokenRequester}.
 *
 * @author matthew.greenfield@opower.com
 */
public class TestOauth2AccessTokenRequester {
    private static final long WONT_EXPIRE_SOON = 20;
    private static final long EXPIRES_SOON = 0;
    private static final AuthorizationCredentials CREDENTIALS = new BasicAuthCredentials("foo", "bar");

    @Rule
    public ExpectedException testRuleExpectedException = ExpectedException.none();
    
    private Oauth2AccessTokenRequester credentialRequester;
    private AccessTokenResource resource;
    
    /**
     * Setup the test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {

        this.resource = EasyMock.createMock(AccessTokenResource.class);
        this.credentialRequester = new Oauth2AccessTokenRequester(this.resource, 1);

    }

    /**
     * Test that credentials are fetched using the client.
     *
     */
    @Test
    public void testCredentialsFetched() {
        expect(this.resource.tokenEndpoint(null, GrantType.CLIENT_CREDENTIALS.alias()))
                .andReturn(createAccessTokenResponse(WONT_EXPIRE_SOON))
                .once();

        replay(this.resource);

        fetchCredentialsXTimes(1);

        verify(this.resource);
    }

    /**
     * Test that credentials previously fetched do not cause the client to be executed.
     *
     */
    @Test
    public void testCredentialsPreviouslyFetched() {
        expect(this.resource.tokenEndpoint(null, GrantType.CLIENT_CREDENTIALS.alias()))
                .andReturn(createAccessTokenResponse(WONT_EXPIRE_SOON))
                .once();

        replay(this.resource);

        fetchCredentialsXTimes(2);

        verify(this.resource);
    }

    /**
     * Test that credentials about to expire are refreshed.
     *
     */
    @Test
    public void testCredentialsAboutToExpire() {
        expect(this.resource.tokenEndpoint(null, GrantType.CLIENT_CREDENTIALS.alias()))
                .andReturn(createAccessTokenResponse(EXPIRES_SOON))
                .times(2);

        replay(this.resource);

        fetchCredentialsXTimes(2);

        verify(this.resource);
    }


    /**
     * Test that an invalid tokenTtlRefresh cannot be passed to the requester.
     */
    @Test
    public void testInvalidTtlRefresh() {
        this.testRuleExpectedException.expect(IllegalStateException.class);
        new Oauth2AccessTokenRequester(this.resource, -1);
    }

    private AuthorizationCredentials fetchCredentialsXTimes(int times) {
        AuthorizationCredentials credentials = null;
        for (int i = 0; i < times; i++) {
            credentials = this.credentialRequester.getAccessToken();
            assertNotNull(credentials);
        }

        return credentials;
    }

    private static AccessTokenResponse createAccessTokenResponse(Long expiresIn) {
        return new AccessTokenResponse("client_id", "access_token", "token_type", ImmutableSet.<Scope>of(), expiresIn);
    }
}
