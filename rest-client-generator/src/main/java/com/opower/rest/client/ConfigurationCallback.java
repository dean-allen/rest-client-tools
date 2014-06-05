package com.opower.rest.client;

/**
 * Configuration callback interface. The ClientBuilder will pass configurable object instances
 * to allow users to customize them as needed.
 * @author chris.phillips
 * @param <T> The type of the object to be configured
 */
public interface ConfigurationCallback<T> {
    /**
     * Apply custom logic to the provided object.
     * @param object The object to be customized.
     */
    void configure(final T object);
}
