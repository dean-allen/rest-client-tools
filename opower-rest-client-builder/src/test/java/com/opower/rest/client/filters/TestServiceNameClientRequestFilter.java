package com.opower.rest.client.filters;

import com.opower.rest.client.generator.core.ClientRequest;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.opower.rest.client.filters.ServiceNameClientRequestFilter.X_SERVICE_NAME;
import static org.easymock.EasyMock.expect;

/**
 * Test for the ServiceNameClientRequestFilter.
 * @author chris.phillips
 */
@RunWith(EasyMockRunner.class)
public class TestServiceNameClientRequestFilter extends EasyMockSupport {

    private static final String SERVICE_NAME = "svcName";

    @TestSubject
    private ServiceNameClientRequestFilter serviceNameClientRequestFilter = new ServiceNameClientRequestFilter(SERVICE_NAME);

    @Mock
    private ClientRequest clientRequest;

    /**
     * Verify the header is added to the request.
     */
    @Test
    public void addsServiceNameToRequest() {
        expect(this.clientRequest.header(X_SERVICE_NAME, SERVICE_NAME))
                .andReturn(this.clientRequest);
        replayAll();
        this.serviceNameClientRequestFilter.filter(this.clientRequest);
        verifyAll();
    }

    /**
     * Verifgy that a null request input throws a NPE.
     */
    @Test(expected = NullPointerException.class)
    public void nullRequestFails() {
        this.serviceNameClientRequestFilter.filter(null);
    }
}
