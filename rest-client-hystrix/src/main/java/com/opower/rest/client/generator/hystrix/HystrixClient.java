package com.opower.rest.client.generator.hystrix;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.opower.rest.client.ConfigurationCallback;
import com.opower.rest.client.generator.core.Client;
import com.opower.rest.client.generator.core.ResourceInterface;
import com.opower.rest.client.generator.core.UriProvider;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class to make the return types of the inherited builders work correctly.
 * @author chris.phillips
 * @param <T> the type of the client to be created
 * @param <B> the type of the concrete builder
 */
public abstract class HystrixClient<T, B extends HystrixClient<T, B>> extends Client<T, B> {
    protected final HystrixCommandGroupKey groupKey;

    // You don't get a fallback by default. You have to provide one
    protected Map<Method, Callable<? extends Object>> fallbackMap = ImmutableMap.of();
    // The default of a thread pool per HystrixCommandKey is sufficient. If you need something different this will allow that.
    protected Map<Method, HystrixThreadPoolKey> threadPoolKeysMap = ImmutableMap.of();

    // we will make default versions of all these and allow people to tweak them with callbacks
    protected final Map<Method, HystrixThreadPoolProperties.Setter> threadPoolPropertiesMap;
    protected final Map<Method, HystrixCommandProperties.Setter> commandPropertiesMap;
    protected Map<Method, HystrixCommandKey> commandKeyMap;

    /**
     * Creates a HystrixClientBuilder with the default HystrixCommand.Setter based on the ResourceClass name.
     *
     * @param resourceInterface The ResourceClass to create a client for
     * @param uriProvider       The uriProvider to use.
     * @param groupKey          The HystrixCommandGroupKey to use
     */
    protected HystrixClient(ResourceInterface<T> resourceInterface,
                            UriProvider uriProvider,
                            HystrixCommandGroupKey groupKey) {
        super(resourceInterface, uriProvider);
        this.groupKey = checkNotNull(groupKey);


        ImmutableMap.Builder<Method, HystrixCommandKey> commandKeyMapBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Method, HystrixCommandProperties.Setter> commandPropertiesMapBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<Method, HystrixThreadPoolProperties.Setter> threadPoolPropertiesBuilder = ImmutableMap.builder();
        for (Method method : resourceInterface.getInterface().getMethods()) {
            commandKeyMapBuilder.put(method, keyForMethod(method));
            // fallbacks disabled till you specify one
            commandPropertiesMapBuilder.put(method, HystrixCommandProperties.Setter().withFallbackEnabled(false));
            threadPoolPropertiesBuilder.put(method, HystrixThreadPoolProperties.Setter());
        }

        this.commandKeyMap = commandKeyMapBuilder.build();
        this.commandPropertiesMap = commandPropertiesMapBuilder.build();
        this.threadPoolPropertiesMap = threadPoolPropertiesBuilder.build();

    }

    /**
     * Generate the HystrixCommandKey for the given method. The command key will be of this format:
     * <p/>
     * < canonicalName of method's declaring class >.< method name>
     *
     * @param method the method to generate a HystrixCommandKey for
     * @return the HystrixCommandKey
     */
    public static HystrixCommandKey keyForMethod(Method method) {
        return HystrixCommandKey.Factory.asKey(String
                                                       .format("%s.%s",
                                                               method.getDeclaringClass().getCanonicalName(),
                                                               method.getName()));
    }

    @SuppressWarnings("unchecked")
    private <P> B applyCallback(Map<Method, P> map, Method method, ConfigurationCallback<P> callback) {
        if (map.containsKey(checkNotNull(method))) {
            checkNotNull(callback).configure(map.get(method));
        } else {
            throw new IllegalArgumentException(String.format("Method %s is not a method on the ResourceInterface", method));
        }
        return (B) this;
    }

    /**
     * Specify custom HystrixCommandProperties for a specific method on the ResourceInterface.
     *
     * @param method   the method to apply the HystrixCommandProperties to
     * @param callback the ConfigurationCallback that applies your custom settings
     * @return the HystrixClientBuilder
     */
    public B methodProperties(Method method, ConfigurationCallback<HystrixCommandProperties.Setter> callback) {
        return applyCallback(this.commandPropertiesMap, method, callback);
    }

    /**
     * Apply custom HystrixCommandProperties for all methods on the ResourceInterface.
     *
     * @param callback the ConfigurationCallback that applies your custom settings to all methods
     * @return the HystrixClientBuilder
     */
    @SuppressWarnings("unchecked")
    public B commandProperties(ConfigurationCallback<HystrixCommandProperties.Setter> callback) {
        for (HystrixCommandProperties.Setter setter : this.commandPropertiesMap.values()) {
            checkNotNull(callback).configure(setter);
        }
        return (B) this;
    }

    /**
     * Specify custom HystrixThreadPoolProperties for a specific method on the ResourceInterface.
     *
     * @param method   the method to apply the HystrixThreadPoolProperties to
     * @param callback the ConfigurationCallback that applies your custom settings
     * @return the HystrixClientBuilder
     */
    public B methodThreadPoolProperties(Method method,
                                        ConfigurationCallback<HystrixThreadPoolProperties.Setter> callback) {
        return applyCallback(this.threadPoolPropertiesMap, method, callback);
    }

    /**
     * Apply custom HystrixThreadPoolProperties for all methods on the ResourceInterface.
     *
     * @param callback the ConfigurationCallback that applies your custom settings to all methods
     * @return the HystrixClientBuilder
     */
    @SuppressWarnings("unchecked")
    public B threadPoolProperties(ConfigurationCallback<HystrixThreadPoolProperties.Setter> callback) {
        for (HystrixThreadPoolProperties.Setter setter : this.threadPoolPropertiesMap.values()) {
            checkNotNull(callback).configure(setter);
        }
        return (B) this;
    }

    /**
     * Specify a custom HystrixCommandKey for a particular method on the ResourceInterface.
     *
     * @param method     the method to use this HystrixCommandKey for
     * @param commandKey the HystrixCommandKey to use
     * @return the HystrixClientBuilder
     */
    @SuppressWarnings("unchecked")
    public B methodCommandKey(Method method, HystrixCommandKey commandKey) {
        this.commandKeyMap = ImmutableMap.<Method, HystrixCommandKey>builder()
                                         .putAll(this.commandKeyMap)
                                         .put(method,
                                              commandKey)
                                         .build();
        return (B) this;
    }

    /**
     * Specify a specific fallback for a particular method on the ResourceClass.
     *
     * @param method   the method that this fallback is to be used for
     * @param fallback the fallback to use
     * @return the HystrixClientBuilder
     */
    @SuppressWarnings("unchecked")
    public B methodFallback(Method method, Callable<?> fallback) {
        this.fallbackMap = ImmutableMap.<Method, Callable<?>>builder().putAll(this.fallbackMap).put(method, fallback).build();
        this.commandPropertiesMap.get(method).withFallbackEnabled(true);
        return (B) this;
    }

    @Override
    public T build() {
        return HystrixCommandInvocationHandler.proxy(this.resourceInterface.getInterface(),
                                                     super.build(),
                                                     ImmutableMap.copyOf(assembleHystrixCommandSetters()),
                                                     ImmutableMap.copyOf(this.fallbackMap));
    }

    private Map<Method, HystrixCommand.Setter> assembleHystrixCommandSetters() {
        return Maps.transformEntries(this.commandKeyMap,
                 new Maps.EntryTransformer<Method, HystrixCommandKey, HystrixCommand.Setter>() {
                    @Override
                    public HystrixCommand.Setter transformEntry(Method method, HystrixCommandKey value) {
                        HystrixClient<T, B> builder = HystrixClient.this;
                        HystrixCommand.Setter setter = HystrixCommand.Setter
                                 .withGroupKey(builder.groupKey)
                                 .andCommandKey(value)
                                 .andCommandPropertiesDefaults(builder.commandPropertiesMap.get(method))
                                 .andThreadPoolPropertiesDefaults(builder.threadPoolPropertiesMap.get(method));
                        if (builder.threadPoolKeysMap.containsKey(method)) {
                            setter.andThreadPoolKey(builder.threadPoolKeysMap.get(method));
                        }
                        return setter;
                    }
                });
    }


    /**
     * ClientBuilder that adds basic Hystrix capabilities to each client instance. The resulting client will use the provided
     * HystrixCommandGroupKey and a HystrixCommandKey per method on the ResourceInterface by default.
     *
     * @author chris.phillips
     * @param <T> The type of the Client we are building
     */
    public static final class Builder<T> extends HystrixClient<T, Builder<T>> {
        /**
         * Creates a HystrixClientBuilder with the default HystrixCommand.Setter based on the ResourceClass name.
         *
         * @param resourceInterface The ResourceClass to create a client for
         * @param uriProvider       The uriProvider to use.
         * @param groupKey          The HystrixCommandGroupKey to use
         */
        public Builder(ResourceInterface resourceInterface, UriProvider uriProvider, HystrixCommandGroupKey groupKey) {
            super(resourceInterface, uriProvider, groupKey);
        }
    }
}
