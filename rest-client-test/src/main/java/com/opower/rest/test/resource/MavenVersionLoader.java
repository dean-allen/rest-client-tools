package com.opower.rest.test.resource;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for looking at the jar file and extracting the maven version information.
 * @author chris.phillips
 */
public class MavenVersionLoader {

    private String groupId;
    private String artifactId;

    /**
     * Create a version loader instance for the given groupId and artifactId.
     * @param groupId the groupId to use
     * @param artifactId the artifactId to use
     */
    public MavenVersionLoader(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Look for the pom.properties file in the jar based on the groupId and artifactId.
     * @return the version string or an empty string
     */
    public String loadVersion() {
        String v = "";
        try {
            Properties p = new Properties();
            InputStream is = getClass().getResourceAsStream(
                    String.format("/META-INF/maven/%s/%s/pom.properties", this.groupId, this.artifactId));
            if (is != null) {
                p.load(is);
                v = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        }

        return v;
    }

}
