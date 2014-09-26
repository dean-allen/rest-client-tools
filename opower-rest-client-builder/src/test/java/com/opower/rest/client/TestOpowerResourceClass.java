package com.opower.rest.client;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.rest.test.validation.resource.ValidTestResource;
import java.util.Arrays;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.eclipse.jetty.server.Server;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Test for OpowerResourceClass.
 * @author chris.phillips
 */
public class TestOpowerResourceClass {

    private static final String[] LOGGED_PACKAGES = {"org.eclipse.jetty", "javax.servlet", "javax.security"};

    /**
     * Make sure the auth-service interface is valid.
     */
    @Test
    public void authResourceIsValid() {
        new OpowerResourceInterface(AccessTokenResource.class);
    }

    /**
     * This ValidTestResource has the various annotations etc that will exercise the validation
     * logic.
     */
    @Test
    public void testResourceInterfaceIsValid() {
        new OpowerResourceInterface(ValidTestResource.class, "com.opower.rest.test.validation.model", "opower.test.package");
    }

    /**
     * Make sure a resource interfaces with unsupported classes logs a warning.
     * This tests a resource interface without the @ResourceMetadata annotation.
     */
    @Test
    public void testUnsupportedClassesAreLogged() {

        Appender mockAppender = createMock(Appender.class);
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        mockAppender.doAppend(logMessageContains(LOGGED_PACKAGES));
        expectLastCall().atLeastOnce();
        replay(mockAppender);
        root.addAppender(mockAppender);
        new OpowerResourceInterface(InvalidResource.class);
        verify(mockAppender);
    }

    private static Object logMessageContains(final String[] expectedStrings) {
        EasyMock.reportMatcher(new IArgumentMatcher() {
            public boolean matches(final Object argument) {
                final String message = ((LoggingEvent) argument).getFormattedMessage();
                return Iterables.any(Arrays.asList(expectedStrings), new Predicate<String>() {
                    public boolean apply(@Nullable String input) {
                        return message.contains(input);
                    }
                });
            }
            public void appendTo(StringBuffer buffer) {
                buffer.append("Log message didn't contain one of [ ")
                      .append(Joiner.on(", ").join(expectedStrings)).append(" ]");

            }
        });
        return null;
    }

    private interface InvalidResource {
        @GET
        Server getServer();
    }
}
