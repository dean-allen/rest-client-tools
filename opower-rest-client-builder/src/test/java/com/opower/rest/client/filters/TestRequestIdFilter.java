package com.opower.rest.client.filters;

import com.opower.rest.client.generator.core.ClientRequest;
import com.opower.rest.client.generator.specimpl.MultivaluedMapImpl;
import javax.ws.rs.core.MultivaluedMap;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;


import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test for the RequestIdFilter.
 * @author chris.phillips
 */
@RunWith(EasyMockRunner.class)
public class TestRequestIdFilter extends EasyMockSupport {

    @TestSubject
    private RequestIdFilter requestIdFilter = new RequestIdFilter();

    @Mock
    private ClientRequest clientRequest;

    /**
     * Adds the UUID to the request.
     */
    @Test
    public void addsUUIDToRequest() {
        expect(this.clientRequest.header(eq(RequestIdFilter.REQUEST_ID_PARAM), anyString()))
                .andReturn(this.clientRequest);
        expect(this.clientRequest.getHeaders()).andReturn(new MultivaluedMapImpl<String, String>());
        replayAll();
        this.requestIdFilter.filter(this.clientRequest);
        verifyAll();
    }

    /**
     * Clears pre-existing request_id header before adding a new oen.
     */
    @Test
    public void removesExistingHeaderThenAddsNewUUID() {
        MultivaluedMap<String, String> headers = new MultivaluedMapImpl<>();
        headers.add(RequestIdFilter.REQUEST_ID_PARAM, RequestIdFilter.REQUEST_ID_PARAM);
        expect(this.clientRequest.header(eq(RequestIdFilter.REQUEST_ID_PARAM), anyString()))
                .andReturn(this.clientRequest);
        expect(this.clientRequest.getHeaders()).andReturn(headers).anyTimes();
        replayAll();
        this.requestIdFilter.filter(this.clientRequest);
        verifyAll();
        assertThat(headers.getFirst(RequestIdFilter.REQUEST_ID_PARAM), not(is(RequestIdFilter.REQUEST_ID_PARAM)));
    }

    /**
     * Throws NPE when input request is null.
     */
    @Test(expected = NullPointerException.class)
    public void nullRequestFails() {
        this.requestIdFilter.filter(null);
    }

}
