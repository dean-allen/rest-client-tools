package com.opower.rest.client.generator.core;

import com.opower.rest.client.generator.util.IsHttpMethod;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents the interface to a JAX-RS Resource that will be shared between servers and clients.
 * @author chris.phillips
 */
public class ResourceInterface<T> {
    private final Class<T> resourceInterface;

    public ResourceInterface(Class<T> resourceInterface) {
        this.resourceInterface = validate(resourceInterface);
    }

    public Class<T> getInterface() {
        return resourceInterface;
    }

    private Class<T> validate(Class<T> resourceInterface) {
        checkNotNull(resourceInterface);

        checkArgument(resourceInterface.isInterface(),
                "The resource class must be an interface with all methods annotated with JAX-RS annotations");

        for (Method m : resourceInterface.getMethods()) {
            checkArgument(IsHttpMethod.isHttpMethod(m),
                    String.format("Method [%s] is not annotated with one or more HttpMethod annotations.", m.getName()));
        }

        return resourceInterface;
    }
}
