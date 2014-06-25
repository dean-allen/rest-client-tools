package com.opower.rest.client.filters.auth;

import com.opower.rest.client.AuthorizationCredentials;
import com.opower.rest.client.generator.core.ClientRequest;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

/**
 * Test for AuthorizationClientRequestFilter.
 * 
 * @author chris.phillips
 */
@RunWith(EasyMockRunner.class)
public class TestAuthorizationClientRequestFilter extends EasyMockSupport {

    private static final String AUTH_HEADER_VALUE = "auth-header-value";
    @TestSubject
    AuthorizationClientRequestFilter authFilter;

    @Mock
    private ClientRequest clientRequest;

    /**
     * The method to set up the filter.
     */
    @Before
    public void setUp() {
        this.authFilter = new AuthorizationClientRequestFilter(new AuthorizationCredentials() {
            @Override
            public String getHttpHeaderValue() {
                return AUTH_HEADER_VALUE;
            }
        });
    }

    /**
     * Ensures the auth header gets added to the request.
     */
    @Test
    public void addsHeaderToRequest() {
        expect(this.clientRequest.header("Authorization", AUTH_HEADER_VALUE))
                .andReturn(this.clientRequest);
        replay(this.clientRequest);
        this.authFilter.filter(this.clientRequest);
        verifyAll();
    }

    /**
     * Ensures a NPE is testRuleExpectedException when null a null request is passed in.
     */
    @Test(expected = NullPointerException.class)
    public void nullRequestFails() {
        this.authFilter.filter(null);
    }
}
