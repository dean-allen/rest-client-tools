package com.opower.rest.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A test object with jackson annotations for testing jackson providers.
 *
 * @author ben.siemon
 * @since 1.1.1
 */
public class TestModelWithFields {
    private final int id;
    private final String name;

    /**
     * Constructs the test type.
     * @param id the id
     * @param name the name
     */
    @JsonCreator
    public TestModelWithFields(
            @JsonProperty("id") int id,
            @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    @JsonProperty("id")
    public int getId() {
        return this.id;
    }

    @JsonProperty("name")
    public String getName() {
        return this.name;
    }
}
