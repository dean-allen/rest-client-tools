package com.opower.rest.client.curator;

import com.google.common.base.Throwables;
import com.opower.rest.client.generator.core.UriProvider;
import java.net.URI;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * UriProvider that uses a curator ServiceDiscovery to resolve the URI each time the getUri method
 * is called.
 * @author chris.phillips
 */
public class CuratorUriProvider implements UriProvider {

    private final ServiceProvider<Void> serviceProvider;
    private final String serviceName;

    /**
     * Creates an instance for the specified serviceName.
     * @param serviceDiscovery the ServiceDiscovery instance to use
     * @param serviceName the serviceName to use. It should match the name which was used to register
     *                    the service in Zookeeper
     */
    public CuratorUriProvider(ServiceDiscovery<Void> serviceDiscovery, String serviceName) {
        this.serviceName = checkNotNull(serviceName);
        this.serviceProvider = checkNotNull(serviceDiscovery).serviceProviderBuilder().serviceName(this.serviceName).build();
        try {
            this.serviceProvider.start();
        } catch (Exception ex) {
            Throwables.propagate(ex);
        }
    }

    /**
     * This is for testing purposes.
     * @param serviceProvider the mock serviceProvider to use
     * @param serviceName the serviceName to use
     */
    CuratorUriProvider(ServiceProvider serviceProvider, String serviceName) {
        this.serviceProvider = serviceProvider;
        this.serviceName = serviceName;
    }

    /**
     * Creates a representative {@link java.net.InetSocketAddress} for the specified
     * {@link org.apache.curator.x.discovery.ServiceInstance}.
     *
     * @param serviceInstance a valid service instance, or null.
     * @return an address for a valid service instance we can connect to.
     */
    private URI createAddressFromInstance(ServiceInstance<Void> serviceInstance)  {
        checkNotNull(serviceInstance, "No instances registered in Zookeeper");
        // TODO: don't hard code http
        return URI.create(String.format("http://%s:%s", serviceInstance.getAddress(), serviceInstance.getPort()));
    }

    @Override
    public URI getUri() {
        try {
            return createAddressFromInstance(this.serviceProvider.getInstance());
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
