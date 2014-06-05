package com.opower.rest.test;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * There's a bug with @FormParam in early versions of Jersey 2. This is a workaround for that.
 * @author chris.phillips
 */
public class AuthAppWithContentTypeBugFix extends AuthApplication {

    @Override
    public Set<Class<?>> getClasses() {
        return ImmutableSet.<Class<?>>builder().addAll(super.getClasses()).add(ContentTypeFilter.class).build();
    }

    /**
     * As per 
     * http://stackoverflow.com/questions/17602432/jersey-and-formparam-not-working-when-charset-is-specified-in-the-content-type
     * and https://java.net/jira/browse/JERSEY-1978.
     */
    @Provider
    @PreMatching
    public static class ContentTypeFilter implements ContainerRequestFilter {

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            MultivaluedMap<String,String> headers = requestContext.getHeaders();
            List<String> contentTypes = headers.remove(HttpHeaders.CONTENT_TYPE);
            if (contentTypes != null && !contentTypes.isEmpty()) {
                String contentType = contentTypes.get(0);
                String sanitizedContentType = contentType.replaceFirst("; charset=UTF-8", "");
                headers.add(HttpHeaders.CONTENT_TYPE, sanitizedContentType);
            }
        }
    }
}
