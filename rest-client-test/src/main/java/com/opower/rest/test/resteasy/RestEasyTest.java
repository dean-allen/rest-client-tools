package com.opower.rest.test.resteasy;

import com.opower.rest.test.FrobTest;
import com.opower.rest.test.jetty.JettyRule;
import com.opower.rest.test.resource.MavenVersionLoader;
import org.junit.ClassRule;

/**
 * This class tests the clients with the varioius RestEasy versions.
 *
 * @author chris.phillips
 */
public class RestEasyTest extends FrobTest {

    @ClassRule
    public static final JettyRule JETTY_RULE = new JettyRule(PORT, RestEasyTest.class.getResource("/web.xml").toString());

    public static final MavenVersionLoader VERSION_LOADER = new MavenVersionLoader("org.jboss.resteasy", "resteasy-jaxrs");

    /**
     * Constructs a test for a specific verson of rest easy.
     *
     * @param expectedVersion the version to test
     */
    public RestEasyTest(String expectedVersion) {
        super(expectedVersion, VERSION_LOADER, "resteasy");
    }

    /**
     * Test for Resteasy 2.0.
     */
    public static class TwoDot0 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot0() {
            super("2.0.1.GA");
        }
    }

    /**
     * Test for Resteasy 2.2.3.GA.
     */
    public static class TwoDot2 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot2() {
            super("2.2.3.GA");
        }
    }

    /**
     * Test for Resteasy 2.3.1.GA.
     */
    public static class TwoDot3Dot1 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot3Dot1() {
            super("2.3.1.GA");
        }
    }

    /**
     * Test for Resteasy 2.3.3.Final.
     */
    public static class TwoDot3Dot3 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot3Dot3() {
            super("2.3.3.Final");
        }
    }

    /**
     * Test for Resteasy 2.3.4.Final.
     */
    public static class TwoDot3Dot4 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot3Dot4() {
            super("2.3.4.Final");
        }
    }

    /**
     * Test for Resteasy 2.3.5.Final.
     */
    public static class TwoDot3Dot5 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot3Dot5() {
            super("2.3.5.Final");
        }
    }

    /**
     * Test for Resteasy 2.3.6.Final.
     */
    public static class TwoDot3Dot6 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot3Dot6() {
            super("2.3.6.Final");
        }
    }

    /**
     * Test for Resteasy 2.3.7.Final.
     */
    public static class TwoDot3Dot7 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public TwoDot3Dot7() {
            super("2.3.7.Final");
        }
    }

    /**
     * Test for Resteasy 3.0.0.Final.
     */
    public static class ThreeDot0Dot0 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public ThreeDot0Dot0() {
            super("3.0.0.Final");
        }
    }

    /**
     * Test for Resteasy 3.0.1.Final.
     */
    public static class ThreeDot0Dot1 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public ThreeDot0Dot1() {
            super("3.0.1.Final");
        }
    }

    /**
     * Test for Resteasy 3.0.3.Final.
     */
    public static class ThreeDot0Dot3 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public ThreeDot0Dot3() {
            super("3.0.3.Final");
        }
    }

    /**
     * Test for Resteasy 3.0.4.Final.
     */
    public static class ThreeDot0Dot4 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public ThreeDot0Dot4() {
            super("3.0.4.Final");
        }
    }

    /**
     * Test for Resteasy 3.0.5.Final.
     */
    public static class ThreeDot0Dot5 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public ThreeDot0Dot5() {
            super("3.0.5.Final");
        }
    }

    /**
     * Test for Resteasy 3.0.6.Final.
     */
    public static class ThreeDot0Dot6 extends RestEasyTest {
        /**
         * Constructs a test.
         */
        public ThreeDot0Dot6() {
            super("3.0.6.Final");
        }
    }


}
