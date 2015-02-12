package com.opower.rest.client.envelope;

import com.google.common.collect.ImmutableMap;
import com.opower.rest.client.generator.core.ClientResponse;
import com.opower.rest.client.generator.util.CaseInsensitiveMap;
import com.opower.rest.client.generator.util.GenericType;
import com.opower.rest.exception.BadRequestException;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Tests for {@link com.opower.rest.client.envelope.EnvelopeErrorInterceptor}.
 *
 * @author ben.siemon
 * @since 1.1.1
 */
public class TestEnvelopeErrorInterceptor {

    @Rule
    public ExpectedException testRuleExpectedException = ExpectedException.none();

    /**
     * Before class setup.
     */
    @BeforeClass
    public static void init() {
        System.setProperty("javax.ws.rs.ext.RuntimeDelegate","com.opower.rest.client.generator.core.BasicRuntimeDelegate");
    }

    /**
     * No header found.
     */
    @Test
    public void testNoHeaderFound() {
        this.testRuleExpectedException.expect(BadRequestException.class);

        EnvelopeErrorInterceptor interceptor = new EnvelopeErrorInterceptor();

        ClientResponse response = createMock(ClientResponse.class);

        expect(response.getHeaders()).andReturn(new CaseInsensitiveMap<String>()).anyTimes();

        expect(response.getEntity(anyObject(Class.class))).andReturn(null).anyTimes();

        expect(response.getStatus()).andReturn(SC_BAD_REQUEST).anyTimes();

        replay(response);

        interceptor.handle(response);

        verify(response);
    }

    /**
     * Header found but nothing at 'error'.
     */
    @Test
    public void testHeaderFoundNoEnvelope() {
        this.testRuleExpectedException.expect(BadRequestException.class);

        MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();

        assertThrowsError(headers, null);

    }

    /**
     * Header found with error.
     */
    @Test
    public void testHeaderFoundWithEnvelope() {
        this.testRuleExpectedException.expect(BadRequestException.class);

        MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        headers.add(EnvelopeJacksonProvider.HEADER_KEY, "v1.0");

        assertThrowsError(
                headers,
                ImmutableMap.of(
                        EnvelopeErrorInterceptor.ERROR_KEY,
                        ImmutableMap.of(
                                EnvelopeErrorInterceptor.ERROR_STATUS_KEY, "400",
                                EnvelopeErrorInterceptor.ERROR_MESSAGE_KEY, "message",
                                EnvelopeErrorInterceptor.ERROR_DETAILS_KEY, "details"))
        );
    }

    private static void assertThrowsError(MultivaluedMap<String, String> headers, Object responseEntity) {

        EnvelopeErrorInterceptor interceptor = new EnvelopeErrorInterceptor();

        ClientResponse response = createMock(ClientResponse.class);

        expect(response.getHeaders()).andReturn(headers).anyTimes();

        expect(response.getEntity(anyObject(GenericType.class))).andReturn(responseEntity).anyTimes();

        expect(response.getEntity(anyObject(Class.class))).andReturn(null).anyTimes();

        expect(response.getStatus()).andReturn(SC_BAD_REQUEST).anyTimes();

        replay(response);

        interceptor.handle(response);

        verify(response);

    }
}
