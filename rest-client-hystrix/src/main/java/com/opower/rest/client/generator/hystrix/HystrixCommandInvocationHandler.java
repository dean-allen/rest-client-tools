package com.opower.rest.client.generator.hystrix;

import com.google.common.base.Throwables;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.opower.rest.client.generator.core.ResourceInterface;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * InvocationHandler that proxies method calls in a HystrixCommand execution.
 *
 * @author chris.phillips
 * @param <T> The type of the Resource
 */
public final class HystrixCommandInvocationHandler<T> implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HystrixCommandInvocationHandler.class);
    private final T target;
    private final Map<Method, Setter> commandSetters;
    private final Map<Method, Callable<?>> fallbacks;

    private HystrixCommandInvocationHandler(T target,
                                            final Map<Method, Setter> commandSetters,
                                            final Map<Method, Callable<?>> fallbacks) {
        this.target = checkNotNull(target);
        this.commandSetters = checkNotNull(commandSetters);
        this.fallbacks = checkNotNull(fallbacks);
    }

    /**
     * All methods will be wrapped in a HystrixCommand set up according to the provided Map of HystrixCommand.Setter.
     *
     * @param resourceInterface  the interface that has methods annotated for JAX-RS resource purposes
     * @param toProxy        the actual resource instance to generator
     * @param commandSetters should you desire to have different configuration for the HystrixCommands per method,
     *                       you can pass that mapping here directly.
     * @param fallbacks The fallbacks to use
     * @param <T>            the type of the resource interface
     * @return a archmage that wraps calls to the underlying resource instance with metrics tracking logic
     */
    @SuppressWarnings("unchecked")
    public static <T> T proxy(ResourceInterface<T> resourceInterface,
                              T toProxy,
                              Map<Method, Setter> commandSetters,
                              Map<Method, Callable<?>> fallbacks) {
        LOG.info("Creating Hystrix based client");
        return (T) Proxy.newProxyInstance(
                toProxy.getClass().getClassLoader(),
                new Class<?>[]{resourceInterface.getInterface()},
                new HystrixCommandInvocationHandler<>(toProxy, commandSetters, fallbacks));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (this.commandSetters.containsKey(method)) {
                return new ProxyCommand(this.commandSetters.get(method), method, args).execute();
            } else {
                return method.invoke(this.target, args);
            }
        } catch (HystrixRuntimeException ex) {
            Throwable t = ex.getCause();
            if (t instanceof InvocationTargetException) {
                throw ((InvocationTargetException) t).getTargetException();
            } else {
                throw t;
            }
        }
    }

    private final class ProxyCommand extends HystrixCommand {

        private final Method toinvoke;
        private final Object[] args;

        private ProxyCommand(Setter setter, Method toinvoke, Object[] args) {
            super(setter);
            this.toinvoke = toinvoke;
            this.args = args;
        }

        @Override
        protected Object run() throws Exception {
            return this.toinvoke.invoke(HystrixCommandInvocationHandler.this.target, this.args);
        }

        @Override
        protected Object getFallback() {

            if (HystrixCommandInvocationHandler.this.fallbacks.containsKey(this.toinvoke)) {
                try {
                    return HystrixCommandInvocationHandler.this.fallbacks.get(this.toinvoke).call();
                } catch (Exception e) {
                    Throwables.propagate(e);
                }
            }
            return super.getFallback();
        }
    }
}
