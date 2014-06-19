package com.opower.rest.test.jersey;

import com.opower.rest.test.FrobTest;
import com.opower.rest.test.jetty.JettyRule;
import com.opower.rest.test.resource.MavenVersionLoader;

import org.junit.ClassRule;

/**
 * Tests how the client interacts with various versions of the Jersey JAX-RX stack. Each test will
 * be executed with only the specified version of Jersey on the classpath and then will use the client
 * under test to make a few requests to the running server (a Jetty server running a Jersey app).
 *
 * @author chris.phillips
 */
public class JerseyTest extends FrobTest {

    private static final String BUG_FIX_TYPE = "jersey/2_content_type_bug";
    
    /**
     * Constructor for sub classes to use.
     *
     * @param versionLoader the MavenVersionLoader to use
     * @param version the version to test
     * @param type    the type is used for resolving which web.xml to use when starting jetty
     *                
     */
    protected JerseyTest(String version, MavenVersionLoader versionLoader, String type) {
        super(version, versionLoader, type);
    }

    /**
     * This class is for versions 1.x of the Jersey framework.
     */
    public static class Jersey1Test extends JerseyTest {
        public static final MavenVersionLoader VERSION_LOADER = new MavenVersionLoader("com.sun.jersey", "jersey-servlet");

        @ClassRule
        public static final JettyRule JETTY_RULE = 
                new JettyRule(PORT, Jersey2Test.class.getResource("/jersey/1/web.xml").toString());

        /**
         * This constructor is for 1.x Jersey tests.
         * @param version the 1.x version to test
         */
        public Jersey1Test(String version) {
            super(version, VERSION_LOADER, "jersey/1");
        }
    }

    /**
     * This class is for versions 2.x of the Jersey framework.
     */
    public static class Jersey2Test extends JerseyTest {
        public static final MavenVersionLoader VERSION_LOADER = new MavenVersionLoader("org.glassfish.jersey.containers",
                                                                                       "jersey-container-servlet-core");
        @ClassRule
        public static final JettyRule JETTY_RULE = 
                new JettyRule(PORT, Jersey2Test.class.getResource("/jersey/2/web.xml").toString());

        /**
         * Constructor for version 2.x Jersey Tests.
         * @param version the 2.x version to use
         */
        public Jersey2Test(String version) {
            super(version, VERSION_LOADER, "jersey/2");
        }

        /**
         * Constructor for version 2.x Jersey Tests.
         * @param version the 2.x version to use
         * @param type the type is used for resolving which web.xml to use when starting jetty
         */
        public Jersey2Test(String version, String type) {
            super(version, VERSION_LOADER, type);
        }
    }

    /**
     * This test exercises version 1.17.1 of the Jersey framework.
     */
    public static class OneDotSeventeenDotOne extends Jersey1Test {
        /**
         * Constructor for this test.
         */
        public OneDotSeventeenDotOne() {
            super("1.17.1");
        }
    }

    /**
     * This test exercises version 1.18.1 of the Jersey framework.
     */
    public static class OneDotEighteenDotOne extends Jersey1Test {
        /**
         * Constructor for this test.
         */
        public OneDotEighteenDotOne() {
            super("1.18.1");
        }
    }

    /**
     * This test exercises version 2.0.1 of the Jersey framework.
     */
    public static class TwoDotODotOne extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotODotOne() {
            super("2.0.1", BUG_FIX_TYPE);
        }
    }

    /**
     * This test exercises version 2.1 of the Jersey framework.
     */
    public static class TwoDotOne extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotOne() {
            super("2.1", BUG_FIX_TYPE);
        }
    }

    /**
     * This test exercises version 2.2 of the Jersey framework.
     */
    public static class TwoDotTwo extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotTwo() {
            super("2.2");
        }
    }

    /**
     * This test exercises version 2.3.1 of the Jersey framework.
     */
    public static class TwoDotThreeDotOne extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotThreeDotOne() {
            super("2.3.1");
        }
    }

    /**
     * This test exercises version 2.4.1 of the Jersey framework.
     */
    public static class TwoDotFourDotOne extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotFourDotOne() {
            super("2.4.1");
        }
    }

    /**
     * This test exercises version 2.5.1 of the Jersey framework.
     */
    public static class TwoDotFiveDotOne extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotFiveDotOne() {
            super("2.5.1");
        }
    }

    /**
     * This test exercises version 2.6 of the Jersey framework.
     */
    public static class TwoDotSix extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotSix() {
            super("2.6");
        }
    }

    /**
     * This test exercises version 2.7 of the Jersey framework.
     */
    public static class TwoDotSeven extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotSeven() {
            super("2.7");
        }
    }

    /**
     * This test exercises version 2.8 of the Jersey framework.
     */
    public static class TwoDotEight extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotEight() {
            super("2.8");
        }
    }

    /**
     * This test exercises version 2.9 of the Jersey framework.
     */
    public static class TwoDotNine extends Jersey2Test {
        /**
         * Constructor for this test.
         */
        public TwoDotNine() {
            super("2.9");
        }
    }


}
