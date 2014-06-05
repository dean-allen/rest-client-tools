package com.opower.rest.test.jetty;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Utility class for creating a Jetty Server instance.
 * @author chris.phillips
 */
public final class JettyServerBuilder {
    
    private JettyServerBuilder() {
        
    }
    
    /**
     * Initialize a Jetty server on the given port using the given web.xml descriptor.
     * @param port the port to listen on
     * @param descriptor the web.xml to use
     * @return the jetty Server instance. It still needs to be started.
     */
    public static Server initServer(int port, String descriptor) {
        Server s = new Server(port);
        HandlerCollection handlerCollection = new HandlerCollection();
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setDescriptor(descriptor);
        webAppContext.setResourceBase("src/test/resources");
        webAppContext.setContextPath("/");
        handlerCollection.addHandler(webAppContext);
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(new NCSARequestLog());
        handlerCollection.addHandler(requestLogHandler);
        s.setHandler(handlerCollection);
        return s;
    }
}
