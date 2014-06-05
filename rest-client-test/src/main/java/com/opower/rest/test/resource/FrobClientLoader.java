package com.opower.rest.test.resource;

import java.util.Map;

/**
 * Allows projects to provide different client instances based on their ClientBuilder implementations.
 * @author chris.phillips
 */
public interface FrobClientLoader {

    /**
     * Assembles a Map of client proxies that will test various configurations of the ClientBuilders.
     * @param port port that the jetty server is listening on
     * @param type type is used to resolve the correct web.xml for any supporting jetty servers
     *             that need to be created for tests. 
     * @return the map of idstring -> test client
     */
    Map<String, FrobResource> clientsToTest(int port, String type);
}
