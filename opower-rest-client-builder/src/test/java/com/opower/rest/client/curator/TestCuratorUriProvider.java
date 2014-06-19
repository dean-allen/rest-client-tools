package com.opower.rest.client.curator;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test for the CuratorUriProvider.
 * @author chris.phillips
 */
@RunWith(EasyMockRunner.class)
public class TestCuratorUriProvider extends EasyMockSupport {

    private static final int PORT = 8080;

    private ServiceProvider<Void> serviceProvider;
    @Mock
    private ServiceInstance<Void> serviceInstance;
    private CuratorUriProvider curatorUriProvider;

    /**
     * Sets up the test fixture.
     */
    @Before
    public void setUp() {
        this.serviceProvider = createNiceMock(ServiceProvider.class);
        this.curatorUriProvider = new CuratorUriProvider(this.serviceProvider, "test");
    }

    /**
     * Ensures proper creation of URIs based on the info provided by the ServiceDiscovery.
     * @throws Exception for covenience
     */
    @Test
    public void createsUri() throws Exception {
        expect(this.serviceProvider.getInstance()).andReturn(this.serviceInstance);
        expect(this.serviceInstance.getAddress()).andReturn("test.com");
        expect(this.serviceInstance.getPort()).andReturn(PORT);
        replayAll();
        assertThat(this.curatorUriProvider.getUri().toString(), is("http://test.com:8080"));
    }

}
