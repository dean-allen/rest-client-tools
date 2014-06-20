package com.opower.rest.client.generator.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
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
 * @param <T> The type of the Resource
 * @author chris.phillips
 */
final class HystrixCommandInvocationHandler<T> implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HystrixCommandInvocationHandler.class);
    private final T target;
    private final Map<Method, HystrixCommand.Setter> commandSetters;
    private final Map<Method, Callable<?>> fallbacks;

    private HystrixCommandInvocationHandler(T target,
                                            final Map<Method, HystrixCommand.Setter> commandSetters,
                                            final Map<Method, Callable<?>> fallbacks) {
        this.target = checkNotNull(target);
        this.commandSetters = checkNotNull(commandSetters);
        this.fallbacks = checkNotNull(fallbacks);
    }

    /**
     * All methods will be wrapped in a HystrixCommand set up according to the provided Map of HystrixCommand.Setter.
     *
     * @param resourceInterface the interface that has methods annotated for JAX-RS resource purposes
     * @param toProxy           the actual resource instance to proxy
     * @param commandSetters    should you desire to have different configuration for the HystrixCommands per method,
     *                          you can pass that mapping here directly.
     * @param fallbacks         The fallbacks to use
     * @param <T>               the type of the resource interface
     * @return a archmage that wraps calls to the underlying resource instance with metrics tracking logic
     */
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> resourceInterface,
                       T toProxy,
                       Map<Method, HystrixCommand.Setter> commandSetters,
                       Map<Method, Callable<?>> fallbacks) {
        LOG.info("Creating Hystrix based client");
        return (T) Proxy.newProxyInstance(
                toProxy.getClass().getClassLoader(),
                new Class<?>[]{resourceInterface},
                new HystrixCommandInvocationHandler<>(toProxy, commandSetters, fallbacks));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (this.commandSetters.containsKey(method)) {
            ProxyCommand command = new ProxyCommand(this.commandSetters.get(method), method, args,
                                                    (Callable<Object>) this.fallbacks.get(method), this.target);
            return execute(command);
        } else {
            return method.invoke(this.target, args);
        }

    }

    /**
     * Executes the command synchronously and throws HystrixRuntimeExceptions for all but cases where there is no fallback
     * configured and a non-hystrix related exception is thrown by the underlying work. Visible for testing
     *
     * @param command the HystrixCommand to execute
     * @return the result of the HystrixCommand
     * @throws Throwable for convenience
     */
    static Object execute(HystrixCommand command) throws Throwable {
        try {
            return command.execute();
        } catch (HystrixRuntimeException ex) {
            // fallback failures should always just throw the HystrixRuntimeException
            if (ex.getFallbackException() != null) {
                throw ex;
            }
            switch (ex.getFailureType()) {
                case COMMAND_EXCEPTION:
                    throw throwCause(ex);
                default:
                    throw ex;
            }
        }
    }

    private static Throwable throwCause(HystrixRuntimeException ex) {
        Throwable cause = ex.getCause();
        if (cause != null) {
            if (cause instanceof InvocationTargetException) {
                return ((InvocationTargetException) cause).getTargetException();
            } else {
                return cause;
            }
        } else {
            return ex;
        }
    }
}
