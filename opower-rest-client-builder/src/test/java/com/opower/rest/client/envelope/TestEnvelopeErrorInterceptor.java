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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for {@link com.opower.rest.client.envelope.EnvelopeErrorInterceptor}.
 *
 * @author ben.siemon
 * @since 1.1.1
 */
public class TestEnvelopeErrorInterceptor {

    public static final String V1_0 = "v1.0";
    public static final String HTTP_400 = "400";
    public static final String MESSAGE = "message";
    public static final String DETAILS = "details";
    public static final String EXCEPTION_EXPECTED = "Should have thrown BadRequestException";
    public static final String SHOULD_BE_400 = "Status code field should be " + HTTP_400;
    public static final String SHOULD_BE_MESSAGE = "Service error code field should be " + MESSAGE;
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
     * Header found with error. Checks the mapping of a simple JSON error structure
     * holding the following fields:
     *
     * - errorMessage (should map to service error code)
     * - httpStatus (should map to status code)
     */
    @Test
    public void testSimpleErrorMapping() {
        try {
            MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
            headers.add(EnvelopeJacksonProvider.HEADER_KEY, V1_0);

            assertThrowsError(
                    headers,
                    ImmutableMap.of(
                            EnvelopeErrorInterceptor.ERROR_KEY,
                            ImmutableMap.of(
                                    EnvelopeErrorInterceptor.ERROR_HTTP_STATUS_KEY, HTTP_400,
                                    EnvelopeErrorInterceptor.ERROR_ERROR_MESSAGE_KEY, MESSAGE))
            );
            fail(EXCEPTION_EXPECTED);
        } catch (BadRequestException e) {
            assertEquals(SHOULD_BE_400, HTTP_400, String.valueOf(e.getStatusCode()));
            assertEquals(SHOULD_BE_MESSAGE, MESSAGE, e.getServiceErrorCode());
            assertEquals("Details field should be null", null, e.getMessage());
        }
    }

    /**
     * Header found with error. Checks the mapping of a detailed JSON error structure
     * holding the following fields:
     *
     * - message (should map to service error code)
     * - status (should map to status code)
     * - details (should map to message)
     */
    @Test
    public void testDetailedErrorMapping() {
        try {
            MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
            headers.add(EnvelopeJacksonProvider.HEADER_KEY, V1_0);

            assertThrowsError(
                    headers,
                    ImmutableMap.of(
                            EnvelopeErrorInterceptor.ERROR_KEY,
                            ImmutableMap.of(
                                    EnvelopeErrorInterceptor.ERROR_STATUS_KEY, HTTP_400,
                                    EnvelopeErrorInterceptor.ERROR_DETAILS_KEY, DETAILS,
                                    EnvelopeErrorInterceptor.ERROR_MESSAGE_KEY, MESSAGE))
            );
            fail(EXCEPTION_EXPECTED);
        } catch (BadRequestException e) {
            assertEquals(SHOULD_BE_400, HTTP_400, String.valueOf(e.getStatusCode()));
            assertEquals(SHOULD_BE_MESSAGE, MESSAGE, e.getServiceErrorCode());
            assertEquals("Details field should be 'details'", DETAILS, e.getMessage());
        }
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
