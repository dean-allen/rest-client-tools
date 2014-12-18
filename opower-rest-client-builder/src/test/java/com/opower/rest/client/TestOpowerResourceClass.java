package com.opower.rest.client;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.opower.auth.resources.oauth2.AccessTokenResource;
import com.opower.rest.client.resource.InvalidResourceMetadataBadName;
import com.opower.rest.client.resource.InvalidResourceMetadataBadVersion;
import com.opower.rest.client.resource.ValidResourceMetadata;
import com.opower.rest.test.validation.resource.ValidTestResource;
import java.util.Arrays;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for OpowerResourceClass.
 * @author chris.phillips
 */
public class TestOpowerResourceClass {

    private static final String[] LOGGED_PACKAGES = {"org.eclipse.jetty", "javax.servlet", "javax.security"};
    private static final ch.qos.logback.classic.Logger ROOT_LOGGER = (ch.qos.logback.classic.Logger) LoggerFactory
            .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

    private Appender mockAppender;
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
     * Setup method.
     */
    @Before
    public void setUp() {
        this.mockAppender = createNiceMock(Appender.class);
        ROOT_LOGGER.addAppender(this.mockAppender);
    }

    /**
     * Tear down method.
     */
    @After
    public void tearDown() {
        ROOT_LOGGER.detachAndStopAllAppenders();
    }

    /**
     * Make sure a resource interfaces with unsupported classes logs a warning.
     * This tests a resource interface without the @ResourceMetadata annotation.
     */
    @Test
    public void testUnsupportedClassesAreLogged() {
        this.mockAppender.doAppend(logMessageContains(LOGGED_PACKAGES));
        expectLastCall().atLeastOnce();
        replay(this.mockAppender);
        new OpowerResourceInterface(InvalidResource.class);
        verify(this.mockAppender);
    }

    /**
     * This interface has a valid ResourceMetadata with modelPackages specified. That should prevent logging of any warnings.
     */
    @Test
    public void testValidResourceMetadata() {
        this.mockAppender.doAppend(anyObject());
        expectLastCall().andAnswer(new IAnswer() {
            public Object answer() {
                fail();
                return null;
            }
        }).anyTimes();
        replay(this.mockAppender);
        OpowerResourceInterface<ValidResourceMetadata> resourceInterface = new OpowerResourceInterface(ValidResourceMetadata.class);
        verify(this.mockAppender);
        assertTrue(resourceInterface.getServiceName().isPresent());
    }

    /**
     * This interface has an invalid ResourceMetadata annotation and should throw an Exception.
     */
    @Test(expected = IllegalStateException.class)
    public void testInvalidResourceMetadataBadVersion() {
        new OpowerResourceInterface(InvalidResourceMetadataBadVersion.class).getServiceName();
    }

    /**
     * This interface has an invalid ResourceMetadata annotation and should throw an Exception.
     */
    @Test(expected = IllegalStateException.class)
    public void testInvalidResourceMetadataBadName() {
        new OpowerResourceInterface(InvalidResourceMetadataBadName.class).getServiceName();
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
