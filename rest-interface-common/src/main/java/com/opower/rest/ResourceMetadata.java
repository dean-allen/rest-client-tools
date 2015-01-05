package com.opower.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by service writers to specify the serviceName and any modelPackages. Simplifies the work of service
 * consumers by not requiring them to gather this information themselves.
 * @author chris.phillips
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)
public @interface ResourceMetadata {

    /**
     * The name the service is registered under excluding the version.
     */
    String serviceName();

    /**
     * The version part of the serviceName.
     */
    int serviceVersion();

    /**
     * Resource interfaces might import model objects from another package that isn't one of the default trusted
     * packages. In that case the ResourceInterface validation would emit a stern warning. By listing these model package
     * names here, the warnings will not appear which will avoid confusing users of the interface.
     */
    String[] modelPackages() default { };
}
