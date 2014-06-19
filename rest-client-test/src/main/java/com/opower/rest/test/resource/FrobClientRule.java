package com.opower.rest.test.resource;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.rules.ExternalResource;


/**
 * Use a java.util.ServiceLoader to find a FrobClientLoader instance to use for the tests. This allows
 * the different versions of the ClientBuilder to assemble different configurations aimed at exercising the
 * different features of the client.
 * @author chris.phillips
 */
public class FrobClientRule extends ExternalResource {

    private static volatile FrobClientLoader clientLoader = load();

    private volatile Map<String, FrobResource> clients;

    private static FrobClientLoader load() {
        if (clientLoader == null) {
            synchronized (FrobClientRule.class) {
                if (clientLoader == null) {
                    clientLoader = init(ServiceLoader.load(FrobClientLoader.class).iterator());
                }
            }
        }
        return clientLoader;
    }

    private static FrobClientLoader init(Iterator<FrobClientLoader> factoryIterator) {

        List<FrobClientLoader> factories = Lists.newArrayList(factoryIterator);
        if (factories.size() == 0) {
            throw new IllegalStateException("No FrobClientLoader detected on classpath.");
        } else {
            return factories.get(0);
        }
    }

    /**
     * Get the specific client instance map for the given test.
     * @param port the port the jetty server is listening on
     * @param type type is used to resolve the correct web.xml to use
     * @return the clientName --> client instance map
     */
    public Map<String, FrobResource> getClientsToTest(int port, String type) {
        if (this.clients == null) {
            synchronized (FrobClientRule.class) {
                if (this.clients == null) {
                    this.clients = clientLoader.clientsToTest(port, type);
                }
            }
        }
        return this.clients;
    }
}
