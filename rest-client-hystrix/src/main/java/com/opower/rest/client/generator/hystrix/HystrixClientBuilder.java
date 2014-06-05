package com.opower.rest.client.generator.hystrix;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.opower.rest.client.generator.core.ClientBuilder;
import com.opower.rest.client.generator.core.ResourceInterface;
import com.opower.rest.client.generator.core.UriProvider;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ClientBuilder that adds basic Hystrix capabilities to each client instance. The resulting client will use a default
 * HystrixCommandGroupKey based on the ResourceClass name. Also, it will use a default HystrixCommand.Setter and no fallback.
 * This can all be configured per method if needed using the customSetter() and customFallback() methods on this builder.
 * 
 * @author chris.phillips
 * @param <T> The type of the Client we are building
 * 
 */
public class HystrixClientBuilder<T> extends ClientBuilder<T> {

    private Map<Method, HystrixCommand.Setter> commandSetters;
    private Map<Method, Callable<?>> fallbacks = ImmutableMap.of();

    /**
     * Creates a HystrixClientBuilder with the default HystrixCommand.Setter based on the ResourceClass name.
     * @param resourceInterface The ResourceClass to create a client for
     * @param uriProvider The uriProvider to use.
     */
    public HystrixClientBuilder(ResourceInterface<T> resourceInterface, UriProvider uriProvider) {
        this(resourceInterface, uriProvider, HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(
                        resourceInterface.getInterface().getCanonicalName())));
    }

    /**
     * Creates a HystrixClientBuilder with the provided HystrixCommandProperties.Setter applied for all the methods
     * on the ResourceClass.
     * @param resourceInterface The ResourceClass to create a client for
     * @param uriProvider The uriProvider to use.
     * @param defaultCommandProperties The commandProperties to apply to all methods
     */
    public HystrixClientBuilder(ResourceInterface<T> resourceInterface, UriProvider uriProvider,
                                HystrixCommandProperties.Setter defaultCommandProperties) {
        this(resourceInterface, uriProvider, HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(
                        resourceInterface.getInterface().getCanonicalName()))
                .andCommandPropertiesDefaults(defaultCommandProperties));

    }

    /**
     * Creates a HystrixClientBuilder using the supplied HystrixCommand.Setter for all the methods on the ResourceClass.
     * @param resourceInterface The ResourceClass to create a client for
     * @param uriProvider The uriProvider to use.
     * @param defaultSetter The HystrixCommand.Setter to apply to all methods
     */
    public HystrixClientBuilder(ResourceInterface<T> resourceInterface, UriProvider uriProvider, 
                                HystrixCommand.Setter defaultSetter) {
        super(resourceInterface, uriProvider);
        this.commandSetters = buildSetterMap(this.resourceInterface.getInterface(), defaultSetter);
    }

    private Map<Method, HystrixCommand.Setter> buildSetterMap(final Class<T> resourceInterface,
                                                              final HystrixCommand.Setter defaultSetter) {
        return Maps.asMap(new HashSet<>(Arrays.asList(resourceInterface.getMethods())),
                          new Function<Method, HystrixCommand.Setter>() {
                    public HystrixCommand.Setter apply(Method method) {
                        return defaultSetter.andCommandKey(keyForMethod(method));
                    }
                });
    }

    /**
     * Generate the HystrixCommandKey for the given method. The command key will be of this format:
     * 
     *      < canonicalName of method's declaring class >.< method name>
     * @param method the method to generate a HystrixCommandKey for
     * @return the HystrixCommandKey
     */
    public static HystrixCommandKey keyForMethod(Method method) {
        return HystrixCommandKey.Factory.asKey(String
                               .format("%s.%s", method.getDeclaringClass().getCanonicalName(), method.getName()));
    }

    /**
     * Specify custom HystrixCommandProperties for a specific method on the ResourceInterface.
     * @param method the method to apply the HystrixCommandProperties to
     * @param propertiesToUse the HystrixCommandProperties to use
     * @return the HystrixClientBuilder
     */
    public HystrixClientBuilder<T> customProperties(Method method, HystrixCommandProperties.Setter propertiesToUse) {
        this.commandSetters.get(method).andCommandPropertiesDefaults(checkNotNull(propertiesToUse));
        return this;
    }

    /**
     * Specify custom HystrixThreadPoolProperties for a specific method on the ResourceInterface.
     * @param method the method to apply the HystrixThreadPoolProperties to
     * @param threadPoolProperties the HystrixThreadPoolProperties to use
     * @return the HystrixClientBuilder
     */
    public HystrixClientBuilder<T> customThreadPoolProperties(Method method, 
                                                              HystrixThreadPoolProperties.Setter threadPoolProperties) {
        this.commandSetters.get(method).andThreadPoolPropertiesDefaults(threadPoolProperties);
        return this;
    }

    /**
     * Specify a specific fallback for a particular method on the ResourceClass.
     * @param method the method that this fallback is to be used for
     * @param fallback the fallback to use
     * @return the HystrixClientBuilder
     */
    public HystrixClientBuilder<T> customFallback(Method method, Callable<?> fallback) {
        this.fallbacks = ImmutableMap.<Method, Callable<?>>builder().putAll(this.fallbacks).put(method, fallback).build();
        return this;
    }

    @Override
    public T build() {
        return HystrixCommandInvocationHandler.proxy(this.resourceInterface,
                                                     super.build(),
                                                     ImmutableMap.copyOf(this.commandSetters),
                                                     ImmutableMap.copyOf(this.fallbacks));
    }
}
