package com.opower.rest.client.generator.hystrix;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.opower.rest.client.generator.hystrix.TestHystrixCommandKeys.TEST_CMD;
import static com.opower.rest.client.generator.hystrix.TestHystrixGroupKeys.TEST_GROUP;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * Tests for the HystrixProxyCommand. Mainly to verify the exception behavior.
 *
 * @author chris.phillips
 */
public class TestProxyCommandExceptionHandling {
    
    private static final String EXCEPTION_MESSAGE = "testMessage";

    @Rule
    public final ExpectedException testRuleExpectedException = ExpectedException.none();

    /**
     * Things that cause the command to fail before the underlying work completes (short circuit, timeout, rejected from thread
     * pool etc) and also have a failed fallback should just propagate the HystrixRuntimeException.
     * @throws Throwable for convenience
     */
    @Test
    public void exceptionInFallbackBeforeWork() throws Throwable {
        TestCommand command = new TestCommand();
        command.setForceCircuitOpen(true);
        command.setFallback(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new RuntimeException(EXCEPTION_MESSAGE);
            }
        });

        this.testRuleExpectedException.expect(HystrixRuntimeException.class);
        this.testRuleExpectedException.expectMessage("TEST_CMD short-circuited and failed retrieving fallback");
        this.testRuleExpectedException.expect(new FallbackExceptionMatcher(RuntimeException.class, EXCEPTION_MESSAGE));
        this.testRuleExpectedException.expect(new FailureTypeMatcher(HystrixRuntimeException.FailureType.SHORTCIRCUIT));

        HystrixCommandInvocationHandler.execute(command);
    }

    /**
     * Things that cause the command to fail before the underlying work completes (see above) should return the 
     * result of a successful fallback.
     * @throws Throwable for convenience
     */
    @Test
    public void successfulFallbackBeforeWorkCompletes() throws Throwable {
        TestCommand command = new TestCommand();
        command.setForceCircuitOpen(true);
        
        int result = (int) HystrixCommandInvocationHandler.execute(command);
        
        assertThat(result, is(0));
        
        
    }

    /**
     * Things that cause a command to fail before the underlying work completes (see above) with no fallback
     * should just propagate the HystrixRuntimeException.
     * @throws Throwable for convenience
     */
    @Test
    public void noFallbackAfterShortCircuit() throws Throwable {
        TestCommand command = new TestCommand();
        command.setForceCircuitOpen(true);
        command.setFallback(null);
        
        this.testRuleExpectedException.expect(HystrixRuntimeException.class);
        this.testRuleExpectedException.expectMessage("TEST_CMD short-circuited and fallback disabled");
        this.testRuleExpectedException.expect(new FailureTypeMatcher(HystrixRuntimeException.FailureType.SHORTCIRCUIT));

        HystrixCommandInvocationHandler.execute(command);
    }

    /**
     * This is a similar test just showing the behavior works regardless of the cause of the pre-work failure. In this case it's
     * a hystrix timeout.
     * @throws Throwable for convenience
     */
    @Test
    public void timeoutNoFallback() throws Throwable {
        TestCommand command = new TestCommand();
        command.setTimeout(1);
        command.setFallback(null);
        command.setBehavior(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                return 1;
            }
        });

        this.testRuleExpectedException.expect(HystrixRuntimeException.class);
        this.testRuleExpectedException.expect(new FailureTypeMatcher(HystrixRuntimeException.FailureType.TIMEOUT));
        this.testRuleExpectedException.expectMessage("TEST_CMD timed-out and fallback disabled.");

        HystrixCommandInvocationHandler.execute(command);
    }

    /**
     * When the underlying work throws, successful fallbacks should be returned.
     * @throws Throwable for convenience
     */
    @Test
    public void exceptionInWorkWithFallbackThatSucceeds() throws Throwable {
        TestCommand command = new TestCommand();
        command.setBehavior(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new RuntimeException();
            }
        });
        
        int result = (int)HystrixCommandInvocationHandler.execute(command);
        assertThat(result, is(0));
    }

    /**
     * When the underlying work throws and the fallback also throws, the HystrixRuntimeException should be propagated.
     * @throws Throwable for convenience
     */
    @Test
    public void exceptionInWorkFallbackFails() throws Throwable {
        TestCommand command = new TestCommand();
        command.setBehavior(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new RuntimeException();
            }
        });
        command.setFallback(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new IllegalStateException(EXCEPTION_MESSAGE);
            }
        });

        this.testRuleExpectedException.expect(HystrixRuntimeException.class);
        this.testRuleExpectedException.expectMessage("TEST_CMD failed and failed retrieving fallback.");
        this.testRuleExpectedException.expect(new FailureTypeMatcher(HystrixRuntimeException.FailureType.COMMAND_EXCEPTION));
        this.testRuleExpectedException.expect(new FallbackExceptionMatcher(IllegalStateException.class, EXCEPTION_MESSAGE));
        HystrixCommandInvocationHandler.execute(command);
    }

    /**
     * In the case of our REST clients, they will always throw InvocationTargetExceptions (since they are jdk proxies). This
     * test is for the case where potentially they throw a different type of exception. In that case that exception should be
     * propagated as is.
     * @throws Throwable for convenience
     */
    @Test
    public void nonProxyExceptionInWorkNoFallback() throws Throwable {
        TestCommand command = new TestCommand();
        command.setBehavior(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new IllegalStateException(EXCEPTION_MESSAGE);
            }
        });
        command.setFallback(null);

        this.testRuleExpectedException.expect(IllegalStateException.class);
        this.testRuleExpectedException.expectMessage(EXCEPTION_MESSAGE);
        
        HystrixCommandInvocationHandler.execute(command);
    }

    /**
     * When the underlying work throws InvocationTargetExceptions (such as in the case of our clients) the target exception 
     * should be this.testRuleExpectedException.
     * @throws Throwable for convenience
     */
    @Test
    public void proxyExceptionInWorkNoFallback() throws Throwable {
        TestCommand command = new TestCommand();
        command.setBehavior(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new InvocationTargetException(new IllegalStateException(EXCEPTION_MESSAGE));
            }
        });
        command.setFallback(null);

        this.testRuleExpectedException.expect(IllegalStateException.class);
        this.testRuleExpectedException.expectMessage(EXCEPTION_MESSAGE);

        HystrixCommandInvocationHandler.execute(command);    
    }

    /**
     * Because of the way hystrix caches settings for commands, we have to do some fancy dancing.
     */
    private class TestCommand extends HystrixCommand {
        private Callable<Object> behavior;
        private Optional<Callable<Object>> fallback;
        
        public TestCommand() {
            super(HystrixCommand.Setter.withGroupKey(TEST_GROUP).andCommandKey(TEST_CMD));
            setTimeout((int)TimeUnit.SECONDS.toMillis(1));
            setForceCircuitOpen(false);
            setFallback(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 0;
                }
            });
            setBehavior(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return 1;
                }
            });
        }

        private String propName(String prop) {
            return String.format("hystrix.command.%s.%s", TEST_CMD.name(), prop);
        }
        
        private void setProp(String name, Object value) {
            Properties props = new Properties();
            props.put(name, value);
            ConfigurationManager.loadProperties(props);    
        }
        
        public void setTimeout(int timeoutInMillis) {
            setProp(propName(".execution.isolation.thread.timeoutInMilliseconds"), timeoutInMillis);   
        }
        
        public void setForceCircuitOpen(boolean forceCircuitOpen) {
            setProp(propName("circuitBreaker.forceOpen"), forceCircuitOpen);
        }

        public void setBehavior(Callable<Object> behavior) {
            this.behavior = behavior;
        }

        public void setFallback(Callable<Object> fallback) {
            this.fallback = Optional.fromNullable(fallback);
            setProp(propName("fallback.enabled"), this.fallback.isPresent());
        }
        
        @Override
        protected Object run() throws Exception {
            return this.behavior.call();
        }

        @Override
        protected Object getFallback() {
            try {
                return this.fallback.get().call();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    private final class FailureTypeMatcher extends TypeSafeMatcher<HystrixRuntimeException> {
        private final HystrixRuntimeException.FailureType expectedType;

        private FailureTypeMatcher(HystrixRuntimeException.FailureType expectedType) {
            this.expectedType = expectedType;
        }

        @Override
        protected boolean matchesSafely(HystrixRuntimeException item) {
            return this.expectedType.equals(item.getFailureType());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("expected failure type [%s] ", this.expectedType));
        }

        @Override
        protected void describeMismatchSafely(HystrixRuntimeException item, Description mismatchDescription) {
            mismatchDescription.appendText(String.format("failure type [%s]", item.getFailureType()));
        }
    }

    private final class FallbackExceptionMatcher extends TypeSafeMatcher<HystrixRuntimeException> {

        private final Class<?> expectedExceptionType;
        private final String expectedMessage;

        private FallbackExceptionMatcher(Class<?> expectedExceptionType, String expectedMessage) {
            this.expectedExceptionType = expectedExceptionType;
            this.expectedMessage = expectedMessage;
        }

        @Override
        protected boolean matchesSafely(HystrixRuntimeException item) {
            return item.getFallbackException() != null
                   && this.expectedExceptionType.equals(item.getFallbackException().getClass())
                   && this.expectedMessage.equals(item.getFallbackException().getMessage());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("expected fallbackException of type [%s] with message [%s]",
                                                 this.expectedExceptionType, this.expectedMessage));
        }

        @Override
        protected void describeMismatchSafely(HystrixRuntimeException item, Description mismatchDescription) {
            mismatchDescription.appendText(String.format("[%s] with message [%s]",
                                                         item.getFallbackException().getClass(),
                                                         item.getFallbackException().getMessage()));
        }
    }
}
