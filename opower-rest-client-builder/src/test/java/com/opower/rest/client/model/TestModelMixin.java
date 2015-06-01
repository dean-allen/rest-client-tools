package com.opower.rest.client.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

/**
 * A Jackson mixin object for tests.
 *
 * @author dean.allen
 */
public abstract class TestModelMixin {

    /**
     * Mixin for getter method.
     * @return name altered by deserializer
     */
    @JsonDeserialize(using = MixinDeserializer.class)
    public abstract String getName();

    /**
     * Simple test deserializer which prepends "mixin: " to the front of a string.
     */
    public static class MixinDeserializer extends JsonDeserializer<String> {
        public static final String PREFIX = "mixin: ";

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            String raw = jp.readValueAs(String.class);
            return PREFIX + raw;
        }
    }
}
