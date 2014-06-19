package com.opower.rest.test.validation.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.ws.rs.ext.Provider;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Dummy model class for testing the validation of OpowerResourceClass instances.
 * @author chris.phillips
 */
@Provider
public class ValidModel {
    /**
     * Field is annotated with hibernate validator annotation to verify that we allow them.
     */
    @NotEmpty
    @JsonProperty
    private boolean ready;

    /**
     * Constructor is annotated with Jackson annotation to verify that we allow them.
     */
    @JsonCreator
    public ValidModel() {

    }
}