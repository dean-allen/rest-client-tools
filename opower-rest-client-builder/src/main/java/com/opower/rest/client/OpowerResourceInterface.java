package com.opower.rest.client;

import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opower.rest.client.generator.core.ResourceInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Attempts to make sure that the interface used for clients only use a certain well defined set of packages.
 *
 * These are the packages that are approved for use in resource interfaces. Classes from any other packages will generate
 * a warning at runtime. We can also not guarantee a solution if your resource interface ignores these warnings and you have
 * problems later.
 *
 *   org.joda.time
 *   javax.ws.rs.core
 *   javax.ws.rs
 *   com.google.common
 *   org.apache.avro
 *   org.codehaus.jackson
 *   com.fasterxml.jackson.annotation
 *   com.fasterxml.jackson.databind.annotation
 *   javax.annotation
 *   javax.validation
 *   org.hibernate.validator.constraints
 *   org.slf4j
 *   com.opower.rest.params
 *
 * @param <T> the type of the resource interface
 * @author chris.phillips
 */
public final class OpowerResourceInterface<T> extends ResourceInterface<T> {

    private static final int MAX_CLASS_CACHE = 300;
    private static final Cache<Class<?>, Boolean> VALIDATED_CLASSES = CacheBuilder.newBuilder()
                                                                                  .maximumSize(MAX_CLASS_CACHE)
                                                                                  .build();
    private static final Logger LOG = LoggerFactory.getLogger(OpowerResourceInterface.class);

    private final Set<String> supportedPackages;

    /**
     * Creates an OpowerResourceInterface instance based on the supplied resource interface class.
     *
     * @param resourceClass the resource interface
     */
    public OpowerResourceInterface(Class<T> resourceClass) {
        this(resourceClass, ImmutableSet.<String>of());
    }

    /**
     * This constructor allows other supporting packages to be accepted when validating the resource. This is useful if
     * you have put dependent classes for your resource interface in different packages. You will receive a stern warning for
     * any classes that you use on your resource interface that are not included in the artifacts listed in rest-interface-base.
     * 
     * @param resourceClass      the resource interface
     * @param modelPackages Set of package names starting with opower or com.opower that contain custom classes referenced
     *                           by the resource class outside of the resource class' package.
     */
    public OpowerResourceInterface(Class<T> resourceClass, String... modelPackages) {
        this(resourceClass, ImmutableSet.copyOf(modelPackages));
    }

    private OpowerResourceInterface(Class<T> resourceClass, Set<String> modelPackages) {
        super(resourceClass);

        Set<String> packageSet = new HashSet<>(checkNotNull(modelPackages));
        checkArgument(Iterables.all(packageSet, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    checkNotNull(input, "Passing null for a supporting package is invalid.");
                    return input.startsWith("com.opower") || input.startsWith("opower");
                }
            }
        ), "supportingPackages must only contain packages that start with opower or com.opower");

        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add("org.joda.time");
        builder.add("javax.ws.rs.core");
        builder.add("javax.ws.rs");
        builder.add("com.google.common");
        builder.add("org.apache.avro");
        builder.add("org.codehaus.jackson");
        builder.add("com.fasterxml.jackson.annotation");
        builder.add("com.fasterxml.jackson.databind.annotation");
        builder.add("javax.annotation");
        builder.add("javax.validation");
        builder.add("org.hibernate.validator.constraints");
        builder.add("org.slf4j");
        builder.add("com.opower.rest.params");
        builder.add(resourceClass.getPackage().getName());
        builder.addAll(packageSet);

        this.supportedPackages = builder.build();

        checkForSupportedTypes(resourceClass);
    }

    private Boolean checkForSupportedTypes(final Class<?> resourceInterface) {
        Boolean present = VALIDATED_CLASSES.getIfPresent(resourceInterface);
        if (present == null) {

            Deque<Class<?>> validationWork = new ArrayDeque<>();
            validationWork.addLast(resourceInterface);

            while (!validationWork.isEmpty()) {
                Class<?> toValidate = validationWork.pop();
                if (VALIDATED_CLASSES.getIfPresent(toValidate) == null) {
                    checkClassAnnotations(toValidate, validationWork);
                    for (Constructor constructor : toValidate.getConstructors()) {
                        checkConstructor(constructor, validationWork);
                    }
                    for (Field f : toValidate.getDeclaredFields()) {
                        checkField(f, validationWork);
                    }

                    for (Method m : toValidate.getMethods()) {
                        if ("equals".equals(m.getName()) || "hashCode".equals(m.getName())) {
                            continue;
                        }
                        checkReturnType(m, validationWork);
                        checkParameterTypes(m, validationWork);
                        checkMethodAnnotations(m, validationWork);
                        checkParameterAnnotations(m, validationWork);
                    }
                    if (!toValidate.equals(resourceInterface)) {
                        checkClass(toValidate);
                    }
                    VALIDATED_CLASSES.put(toValidate, Boolean.TRUE);
                }
            }
            return true;
        } else {
            return present;
        }
    }

    private void checkField(Field f, Deque<Class<?>> validationWork) {
        addWork(f.getType(), validationWork);
        for (Annotation annotation : f.getAnnotations()) {
            addWork(annotation.annotationType(), validationWork);
        }
    }

    private void checkConstructor(Constructor constructor, Deque<Class<?>> validationWork) {
        for (Annotation annotation : constructor.getAnnotations()) {
            addWork(annotation.annotationType(), validationWork);
        }
    }

    private void addWork(Class<?> toAdd, Deque<Class<?>> validationWork) {
        if (toAdd.isPrimitive() || (toAdd.getPackage() != null && toAdd.getPackage().getName().startsWith("java."))) {
            return;
        }
        if (toAdd.isArray()) {
            addWork(toAdd.getComponentType(), validationWork);
            return;
        }
        if (VALIDATED_CLASSES.getIfPresent(toAdd) == null) {
            validationWork.addLast(toAdd);
            if (toAdd.getSuperclass() != null) {
                addWork(toAdd.getSuperclass(), validationWork);
            }
        }
    }

    private void checkClass(final Class<?> toCheck) {
        Predicate<String> isClassToCheckInPackage = new Predicate<String>() {
            @Override
            public boolean apply(String supportedPackage) {
                return toCheck.getPackage().getName().startsWith(supportedPackage);
            }
        };

        if (!Iterables.any(this.supportedPackages, isClassToCheckInPackage)) {
            LOG.warn("You are using a potentially unsafe type that may cause classpath conflicts!! [ {} ]",
                     toCheck.getCanonicalName());
        }
    }

    private void checkClassAnnotations(Class<?> toCheck, Deque<Class<?>> validationWork) {
        for (Annotation a : toCheck.getAnnotations()) {
            addWork(a.annotationType(), validationWork);
        }
    }

    private void checkParameterAnnotations(Method m, Deque<Class<?>> validationWork) {
        for (int i = 0; i < m.getParameterAnnotations().length; i++) {
            for (int j = 0; j < m.getParameterAnnotations()[i].length; j++) {
                Class<?> toCheck = m.getParameterAnnotations()[i][j].annotationType();
                addWork(toCheck, validationWork);
            }
        }
    }

    private void checkMethodAnnotations(Method m, Deque<Class<?>> validationWork) {
        for (int i = 0; i < m.getAnnotations().length; i++) {
            Class<?> toCheck = m.getAnnotations()[i].annotationType();
            addWork(toCheck, validationWork);
        }
    }

    private void checkParameterTypes(Method m, Deque<Class<?>> validationWork) {
        for (Class<?> c : m.getParameterTypes()) {
            addWork(c, validationWork);
        }
    }

    private void checkReturnType(Method m, Deque<Class<?>> validationWork) {
        addWork(m.getReturnType(), validationWork);
    }

}
