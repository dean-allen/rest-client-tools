package com.opower.rest.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.opower.rest.client.generator.core.ClientBuilder;
import com.opower.rest.client.generator.core.ResourceInterface;
import com.opower.rest.client.generator.core.SimpleUriProvider;
import com.opower.rest.client.generator.executors.ApacheHttpClient4Executor;
import com.opower.rest.test.resource.FrobClientLoader;
import com.opower.rest.test.resource.FrobResource;

import java.util.Map;

/**
 * @author chris.phillips
 */
public class BasicFrobClientLoader implements FrobClientLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setDateFormat(new ISO8601DateFormat())
            .registerModule(new GuavaModule())
            .registerModule(new JodaModule());
    private static final JacksonJsonProvider JACKSON_JSON_PROVIDER = new JacksonJsonProvider(OBJECT_MAPPER)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public Map<String, FrobResource> clientsToTest(final int port, String type) {
        try {
            ClientBuilder<FrobResource> clientBuilder = ClientBuilder.instance(new ResourceInterface<>(FrobResource.class),
                    new SimpleUriProvider(String.format("http://localhost:%s/", port)))
                    .executor(new ApacheHttpClient4Executor()).messageBodyProviders(JACKSON_JSON_PROVIDER, JACKSON_JSON_PROVIDER);
            return ImmutableMap.of("default", clientBuilder.build());
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }
}
