package com.opower.rest.test.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import java.util.Random;
import org.joda.time.LocalDate;


/**
 * Simple entity used for exercising JAX-RS implementations.
 * @author chris.phillips
 */
public class Frob {
    
    private static final int MAX_RAND = 100;
    private static final int HALF_MAX = MAX_RAND / 2;

    @JsonProperty
    private String id;

    @JsonProperty
    private Integer counter = new Random(MAX_RAND).nextInt();

    @JsonProperty
    private Optional<Long> optionalField = this.counter < HALF_MAX ? Optional.<Long>absent() : Optional.of(new Random().nextLong());

    @JsonProperty
    private LocalDate localDate = new LocalDate();

    /**
     * Simple constructor.
     */
    public Frob() {

    }

    /**
     * Construct a Frob with the given id.
     * @param id id to use
     */
    public Frob(String id) {
        this.id = id;
    }

    /**
     * This optional field will make sure that introducing Optional fields to classes will work at 
     * runtime.
     * @return the sample optionalField
     */
    public Optional<Long> getOptionalField() {
        return this.optionalField;
    }

    /**
     * This LocalDate field will ensure that introducing joda classes to the various client tests will 
     * work at runtime.
     * @return the sample localDate field
     */
    public LocalDate getLocalDate() {
        return this.localDate;
    }

    /**
     * Get the id of the Frob instance.
     * @return the id.
     */
    public String getId() {
        return this.id;
    }

    /**
     * A simple integer field.
     * @return the value of counter
     */
    public Integer getCounter() {
        return this.counter;
    }
}
