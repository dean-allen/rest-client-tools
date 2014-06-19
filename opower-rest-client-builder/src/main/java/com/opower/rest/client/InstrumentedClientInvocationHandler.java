package com.opower.rest.client;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.opower.metrics.MetricsProvider;
import com.opower.metrics.MetricsProvider.TimingContext;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

/**
 * InvocationHandler that wraps the target JAX-RS Resource client with automatic tracking of metrics.
 *
 * @param <T> The type of the resource that is being tracked
 * @author chris.phillips@opower.com
 */
public class InstrumentedClientInvocationHandler<T> implements InvocationHandler {

    private final MetricsProvider metricsProvider;
    private final T target;
    private final Set<String> instrumentedMethods;
    private final LoadingCache<Method, String> metricKeys = CacheBuilder.newBuilder().build(
            new CacheLoader<Method, String>() {
                @Override
                public String load(Method method) throws Exception {
                    return String.format("%s.%s", method.getDeclaringClass().getCanonicalName(), method.getName());
                }
            });
    private final LoadingCache<Method, String> exceptionMetricKeys = CacheBuilder.newBuilder().build(
            new CacheLoader<Method, String>() {
                @Override
                public String load(Method method) throws Exception {
                    return String.format("%s-Exception", InstrumentedClientInvocationHandler.this.metricKeys.get(method));
                }
            });

    /**
     * Constructor intended for testing purposes only.
     * @param metricsProvider the MetricsProvider that will track the performance and exceptions of the proxied resource
     * @param resourceClass the interface that has methods annotated for JAX-RS resource purposes
     * @param target the actual resource instance to generator
     */
    protected InstrumentedClientInvocationHandler(MetricsProvider metricsProvider, Class<T> resourceClass, T target) {
        this.metricsProvider = metricsProvider;
        this.target = target;

        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (Method m : resourceClass.getMethods()) {
            if (!isJaxRSAnnotated(m)) {
                continue;
            }
            builder.add(m.getName());
        }
        this.instrumentedMethods = builder.build();
    }

    /**
     * Wrap the given resource with automatic tracking of metrics using the given MetricsProvider.
     *
     * @param resourceClass   the interface that has methods annotated for JAX-RS resource purposes
     * @param toProxy         the actual resource instance to generator
     * @param metricsProvider the MetricsProvider that will track the performance and exceptions of the proxied resource
     * @param <T>             the type of the resource interface
     * @return a archmage that wraps calls to the underlying resource instance with metrics tracking logic
     */
    @SuppressWarnings("unchecked")
    public static <T> T instrument(Class<T> resourceClass, T toProxy, MetricsProvider metricsProvider) {
        return (T) Proxy.newProxyInstance(
                toProxy.getClass().getClassLoader(),
                new Class<?>[]{resourceClass},
                new InstrumentedClientInvocationHandler<>(metricsProvider, resourceClass, toProxy));
    }

    /**
     * Checks a Method to see if it is annotated with one of the jax-rs Resource annotations.
     *
     * @param m the method to check
     * @return true if one of the annotations is found on the method, false otherwise
     */
    public static boolean isJaxRSAnnotated(Method m) {
        return m.isAnnotationPresent(GET.class)
                || m.isAnnotationPresent(POST.class)
                || m.isAnnotationPresent(PUT.class)
                || m.isAnnotationPresent(DELETE.class)
                || m.isAnnotationPresent(OPTIONS.class)
                || m.isAnnotationPresent(HEAD.class);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (this.instrumentedMethods.contains(method.getName())) {
            final TimingContext context =
                    this.metricsProvider.time(this.metricKeys.get(method));
            try {
                return method.invoke(this.target, args);
            } catch (InvocationTargetException e) {
                this.metricsProvider.mark(this.exceptionMetricKeys.get(method));
                throw Throwables.propagate(e.getTargetException());
            } finally {
                context.stop();
            }
        } else {
            return method.invoke(this.target, args);
        }
    }

}
