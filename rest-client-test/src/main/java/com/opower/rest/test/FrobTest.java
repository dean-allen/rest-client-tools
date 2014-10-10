package com.opower.rest.test;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.opower.rest.test.resource.Frob;
import com.opower.rest.test.resource.FrobClientRule;
import com.opower.rest.test.resource.FrobResource;
import com.opower.rest.test.resource.MavenVersionLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Base class for the various tests.
 *
 * @author chris.phillips
 */
public abstract class FrobTest {

    @ClassRule
    public static final FrobClientRule CLIENT_RULE = new FrobClientRule();
    protected static final int PORT = 7000;
    private static final Logger LOG = LoggerFactory.getLogger(FrobTest.class);
    private static final String TEST_ID = "testId";

    private final MavenVersionLoader versionLoader;
    private final String version;
    private final String type;

    /**
     * Constructor for sub classes to use.
     *
     * @param version       the version to test
     * @param versionLoader the MavenVersionLoader to use
     * @param type          the type is used for resolving which web.xml to use when starting jetty
     */
    protected FrobTest(String version, MavenVersionLoader versionLoader, String type) {
        this.version = version;
        this.versionLoader = versionLoader;
        this.type = type;
    }

    /**
     * This test checks the expected version against the version found inside the jersey jar.
     */
    @Test
    public void weAreUsingTheCorrectVersion() {
        assertThat(this.versionLoader.loadVersion(), is(this.version));
    }

    /**
     * This test uses each of the clientsToTest from the CLIENT_RULE to do a simple GET with a @PathParam.
     */
    @Test
    public void testGet() {
        testClients(new FrobTester() {
            @Override
            public void doTest(FrobResource frobResource) {
                Frob f = frobResource.findFrob(TEST_ID);
                Assert.assertThat(f.getId(), is(TEST_ID));
            }
        });
    }

    /**
     * This test uses each of the clientsToTest from the CLIENT_RULE to do a simple POST with a @FormParam.
     */
    @Test
    public void testPOSTWithFormParam() {
        testClients(new FrobTester() {
            @Override
            public void doTest(FrobResource frobResource) {
                Frob f = frobResource.updateFrob(TEST_ID, "newName");
                Assert.assertThat(f.getId(), is(TEST_ID));
            }
        });
    }

    /**
     * This test uses each of the clientsToTest from the CLIENT_RULE to do a simple PUT with a Frob
     * object serialized as the request body.
     */
    @Test
    public void testCreate() {
        testClients(new FrobTester() {
            @Override
            public void doTest(FrobResource frobResource) {
                Response response = frobResource.createFrob(new Frob("testCreate"));
                Assert.assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
                try {
                    assertThat(CharStreams.toString(new InputStreamReader((InputStream) response.getEntity(),
                                                                          Charsets.UTF_8)), is("success"));
                } catch (IOException e) {
                    Throwables.propagate(e);
                }
            }
        });
    }

    /**
     * Exercises the frobString method.
     */
    @Test
    public void testFrobString() {
        testClients(new FrobTester() {
            @Override
            public void doTest(FrobResource frobResource) {
                String echo = frobResource.frobString("hello!");
                Assert.assertThat(echo, is("You sent hello!"));
            }
        });
    }

    private void testClients(FrobTester tester) {
        for (Map.Entry<String, FrobResource> entry : CLIENT_RULE.getClientsToTest(PORT, this.type).entrySet()) {
            LOG.info("Testing client: " + entry.getKey());
            tester.doTest(entry.getValue());
        }
    }

    /**
     * Callback interface for Frob tests.
     */
    protected interface FrobTester {
        /**
         * Perform the actual work with the frobResource.
         *
         * @param frobResource the client to test
         */
        void doTest(FrobResource frobResource);
    }
}
