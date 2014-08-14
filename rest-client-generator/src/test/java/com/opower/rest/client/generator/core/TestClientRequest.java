package com.opower.rest.client.generator.core;

import com.google.common.base.Optional;
import com.opower.rest.client.generator.specimpl.MultivaluedMapImpl;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ClientRequest}
 * @author sachin.nene
 */
public class TestClientRequest {

    private ClientRequest request;
    private MultivaluedMap<String,String> expectedValues = new MultivaluedMapImpl<>();

    @Before
    public void setUp() {
        request = new ClientRequest("http://dummy", null, null);
    }

    @Test
    public void testFormParameterOptionalPresent() {
        this.expectedValues.putSingle("key1", "asdf");
        this.request.formParameter("key1", Optional.of("asdf"));
        assertEquals("Form parameters don't match.", this.expectedValues, this.request.getFormParameters());
    }

    @Test
    public void testQueryParameterOptionalPresent() {
        this.expectedValues.putSingle("key1", "asdf");
        this.request.queryParameter("key1", Optional.of("asdf"));
        assertEquals("Query parameters don't match.", this.expectedValues, this.request.getQueryParameters());
    }

    @Test
    public void testMatrixParameterOptionalPresent() {
        this.expectedValues.putSingle("key1", "asdf");
        this.request.matrixParameter("key1", Optional.of("asdf"));
        assertEquals("Matrix parameters don't match.", this.expectedValues, this.request.getMatrixParameters());
    }

    @Test
    public void testFormParameterOptionalAbsent() {
        this.request.formParameter("key1", Optional.absent());
        assertEquals("Form parameters don't match.", this.expectedValues, this.request.getFormParameters());
    }

    @Test
    public void testQueryParameterOptionalAbsent() {
        this.request.queryParameter("key1", Optional.absent());
        assertEquals("Query parameters don't match.", this.expectedValues, this.request.getQueryParameters());
    }

    @Test
    public void testMatrixParameterOptionalAbsent() {
        this.request.matrixParameter("key1", Optional.absent());
        assertEquals("Matrix parameters don't match.", this.expectedValues, this.request.getMatrixParameters());
    }

}