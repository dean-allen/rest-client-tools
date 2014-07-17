package com.opower.rest.client;

import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.opower.rest.client.generator.core.ResourceInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Attempts to make sure that the interface used for clients only use a certain well defined set of packages.
 *
 * @param <T> the type of the resource interface
 * @author chris.phillips
 */
public class OpowerResourceInterface<T> extends ResourceInterface<T> {

    private static final int MAX_CLASS_CACHE = 300;
    private static final Cache<Class<?>, Boolean> VALIDATED_CLASSES = CacheBuilder.newBuilder()
                                                                                  .maximumSize(MAX_CLASS_CACHE)
                                                                                  .build();

    private final Set<String> supportedPackages;

    /**
     * Use this constructor if all the supporting classes for your interface are in the same package as the interface.
     *
     * @param resourceClass the resource interface
     */
    public OpowerResourceInterface(Class<T> resourceClass) {
        this(resourceClass, resourceClass.getPackage().getName());
    }

    /**
     * This constructor allows other supporting packages to be accepted when validating the resource. This is useful if
     * you have put dependent classes for your resource interface in different packages. Supporting packages must start with
     * com.opower
     * 
     * Consider for an example that you're writing a User service. You have a UserResource interface that looks like this:
     * 
     * package com.opower.service;
     * 
     * import com.opower.model.user.CreateUserResponse;
     * import com.opower.model.user.CreateUserRequest;
     * 
     * @Path("/users")
     * public interface UserResource {
     *      @PUT
     *      CreateUserResponse createUser(CreateUserRequest request);
     * }
     *
     * The interface and supporting request / response objects are all packaged together. The validation logic will automatically
     * trust all classes in the com.opower.service package (the package of the resource interface). But the supporting objects
     * are found in a separate package and so you must pass "com.opower.model.user" to the constructor so validation will succeed.
     * 
     * @param resourceClass      the resource interface
     * @param supportingPackages Set of package names starting with com.opower that contain custom classes referenced
     *                           by the resource class outside of the resource class' package.
     */
    public OpowerResourceInterface(Class<T> resourceClass, String... supportingPackages) {
        super(resourceClass);

        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add("org.joda.time");
        builder.add("javax.ws.rs.core");
        builder.add("javax.ws.rs");
        builder.add("com.google.common");
        builder.add("com.fasterxml.jackson.annotation");
        builder.add("com.fasterxml.jackson.databind.annotation");
        builder.add("javax.annotation");
        builder.add("javax.validation");
        builder.add("org.hibernate.validator.constraints");
        builder.add("org.slf4j");

        Set<String> packageSet = Sets.newHashSet(supportingPackages);
        checkArgument(Iterables.all(packageSet, new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    return input.startsWith("com.opower");
                }
            }
        ));

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
        checkArgument(Iterables.any(this.supportedPackages, new Predicate<String>() {
                @Override
                public boolean apply(String supportedPackage) {
                    return toCheck.getPackage().getName().startsWith(supportedPackage);
                }
            }),
                      String.format("Type %s is not supported", toCheck.getCanonicalName()));
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
