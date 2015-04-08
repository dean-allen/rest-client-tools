package com.opower.rest.client.envelope;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Locale;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import opower.util.DateUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Attempts to unwrap a response entity from a json envelope. If the X-OPOWER-JsonEnvelope header is found we expect to find
 * the response entity nested in a json object at key 'response'. If no header is found we delegate to the default
 * {@link com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider} behavior.
 *
 * Part of the X-OPOWER-JSONEnvelope specification allows for errors to be sent in a nested object under an 'error' key. This
 * is handled upstream in an {@link com.opower.rest.client.generator.core.ClientErrorInterceptor} found here:
 * {@link com.opower.rest.client.envelope.EnvelopeErrorInterceptor}.
 *
 * @author ben.siemon
 * @since 1.1.1
 */
public class EnvelopeJacksonProvider extends JacksonJsonProvider {
    /**
     * HTTP header indicating that a response has been wrapped.  Since this guy is on the hook for all reading and writing of
     * JSON, we have to be able to distinguish between calls that have been wrapped, versus ones that have not.  So we use this
     * header to do that.
     */
    public static final String HEADER_KEY = "X-OPOWER-JsonEnvelope";
    public static final String RESPONSE_KEY = "response";

    /**
     * Constructs the provider and configures the inherited {@link com.fasterxml.jackson.databind.ObjectMapper}.
     */
    public EnvelopeJacksonProvider() {
        super();
        overrideDefaultConfig();
    }

    /**
     * Configures the inherited {@link com.fasterxml.jackson.databind.ObjectMapper}.
     */
    protected void overrideDefaultConfig() {
        configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);

        ObjectMapper mapper = _mapperConfig.getConfiguredMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setDateFormat(new SimpleDateFormat(DateUtils.YYYYMMDD, Locale.US));
    }

    @Override
    public Object readFrom(Class<Object> type,
                           Type genericType,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream) throws IOException {
        InputStream stream;
        if (httpHeaders.containsKey(HEADER_KEY)) {
            JsonNode root = readTree(entityStream);

            root = root.has(RESPONSE_KEY) ? root.get(RESPONSE_KEY) : root;
            stream = new ByteArrayInputStream(writeTree(root));
        } else {
            stream = entityStream;
        }
        return super.readFrom(
                type,
                genericType,
                annotations,
                mediaType,
                httpHeaders,
                stream);
    }

    /**
     * Parses a {@link com.fasterxml.jackson.databind.JsonNode} from the provided {@link java.io.InputStream}.
     * @param stream a non null input stream containing response json.
     * @return a {@link com.fasterxml.jackson.databind.JsonNode} representing the json test from stream.
     * @throws IOException thrown on json processing failure.
     */
    protected JsonNode readTree(InputStream stream) throws IOException {
        checkNotNull(stream, "stream");
        return _mapperConfig.getConfiguredMapper().getFactory().createParser(stream).readValueAsTree();
    }

    /**
     * Convets the provided {@link com.fasterxml.jackson.databind.JsonNode} to a UTF encoded byte array.
     *
     * @param root the json document to encode
     * @return a UTF encoded byte array of 'root'
     * @throws IOException thrown on json processing failure.
     */
    protected byte[] writeTree(JsonNode root) throws IOException {
        checkNotNull(root, "root");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        _mapperConfig.getConfiguredMapper().getFactory().createGenerator(out).writeTree(root);
        return out.toByteArray();
    }
}
