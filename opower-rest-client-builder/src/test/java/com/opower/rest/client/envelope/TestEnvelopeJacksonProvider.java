package com.opower.rest.client.envelope;

import com.opower.rest.client.generator.util.CaseInsensitiveMap;
import com.opower.rest.client.model.TestModelWithFields;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link com.opower.rest.client.envelope.EnvelopeJacksonProvider}.
 *
 * @author ben.siemon
 * @since 1.1.1
 */
public class TestEnvelopeJacksonProvider {
    private static final String ASSERT_ERROR = "Wrong object extracted.";
    private static final String NAME_FIELD = "name";
    private static final String UTF_8 = "UTF-8";

    /**
     * Before class setup.
     */
    @BeforeClass
    public static void init() {
        System.setProperty("javax.ws.rs.ext.RuntimeDelegate","com.opower.rest.client.generator.core.BasicRuntimeDelegate");
    }
 
    /**
     * Tests that the {@link com.opower.rest.client.envelope.EnvelopeJacksonProvider} handles the non wrapped case.
     * @throws IOException on I/0 failure
     */
    @Test
    public void testNoHeaderUsesDefaultBehavior() throws IOException {
        EnvelopeJacksonProvider provider = new EnvelopeJacksonProvider();

        MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();

        @SuppressWarnings("unchecked")
        Class<Object> klass = (Class<Object>) (Class) TestModelWithFields.class;

        TestModelWithFields model = (TestModelWithFields) provider.readFrom(
                klass,
                klass,
                new Annotation[0],
                MediaType.APPLICATION_JSON_TYPE,
                headers,
                testData(0, NAME_FIELD));

        assertEquals(ASSERT_ERROR, NAME_FIELD, model.getName());
        assertEquals(ASSERT_ERROR, 0, model.getId());
    }

    /**
     * Tests that the {@link com.opower.rest.client.envelope.EnvelopeJacksonProvider} handles the wrapped case.
     * @throws IOException on I/0 failure
     */
    @Test
    public void testUnwrapsWithHeader() throws IOException {
        EnvelopeJacksonProvider provider = new EnvelopeJacksonProvider();

        MultivaluedMap<String, String> headers = new CaseInsensitiveMap<>();
        headers.add(EnvelopeJacksonProvider.HEADER_KEY, "v1.0");

        @SuppressWarnings("unchecked")
        Class<Object> klass = (Class<Object>) (Class) TestModelWithFields.class;

        TestModelWithFields model = (TestModelWithFields) provider.readFrom(
                klass,
                klass,
                new Annotation[0],
                MediaType.APPLICATION_JSON_TYPE,
                headers,
                testDataInEnvelope(0, NAME_FIELD));

        assertEquals(ASSERT_ERROR, NAME_FIELD, model.getName());
        assertEquals(ASSERT_ERROR, 0, model.getId());
    }


    /**
     * Prepares vanilla test data.
     * @param id
     * @param name
     * @return
     * @throws IOException on I/0 failure
     */
    private static InputStream testData(int id, String name) throws IOException {
        return new ByteArrayInputStream(String.format("{\"id\":%s ,\"name\":\"%s\"}", id, name)
                .getBytes(UTF_8));
    }

    /**
     * Creates wrapped test data.
     * @param id
     * @param name
     * @return
     * @throws IOException on I/0 failure
     */
    private static InputStream testDataInEnvelope(int id, String name) throws IOException {
        return new ByteArrayInputStream(String.format(
                "{\"" + EnvelopeJacksonProvider.RESPONSE_KEY + "\":{\"id\":%s ,\"name\":\"%s\"}}", id, name)
                .getBytes(UTF_8));
    }
}
