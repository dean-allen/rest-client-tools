package com.opower.rest.client.filters.auth;

import com.google.common.net.HttpHeaders;
import com.opower.auth.oauth2.BearerToken;
import com.opower.rest.client.BearerAuthCredentials;
import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.core.ClientRequestFilter;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Unit tests for the {@link Oauth2CredentialRequestingClientRequestFilter}.
 *
 * @author matthew.greenfield@opower.com
 */
public class TestCredentialRequestingClientRequestFilter {
    private static final String ACCESS_TOKEN = "foobar";
    private static final BearerAuthCredentials CREDENTIALS = new BearerAuthCredentials(new BearerToken(ACCESS_TOKEN));

    @Rule
    public ExpectedException testRuleExpectedException = ExpectedException.none();

    /**
     * Test that the {@link Oauth2AccessTokenRequester} is called and the credentials returned are added to the request.
     */
    @Test
    public void testCredentialsRequested() {
        Oauth2AccessTokenRequester credentialRequester = EasyMock.createMockBuilder(Oauth2AccessTokenRequester.class)
                                                                .addMockedMethod("getAccessToken")
                                                                .createMock();
        expect(credentialRequester.getAccessToken()).andReturn(CREDENTIALS).once();

        ClientRequest request = createMock(ClientRequest.class);
        expect(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)).andReturn(request);
        expectLastCall().once();

        ClientRequestFilter clientRequestFilter = new Oauth2CredentialRequestingClientRequestFilter(credentialRequester);

        replay(request, credentialRequester);

        clientRequestFilter.filter(request);

        verify(request, credentialRequester);
    }

    /**
     * Test that a {@link Oauth2CredentialRequestingClientRequestFilter} cannot be created with a {@code null} 
     * {@link Oauth2AccessTokenRequester}.
     */
    @Test
    public void testNullRequester() {
        this.testRuleExpectedException.expect(NullPointerException.class);
        new Oauth2CredentialRequestingClientRequestFilter(null);
    }
}
