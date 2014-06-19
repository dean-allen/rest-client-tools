package com.opower.rest.client.generator.core;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author chris.phillips
 */
public class SimpleUriProvider implements UriProvider {

    private final URI uri;

    public SimpleUriProvider(URI uri) {
        this.uri = checkNotNull(uri);
    }

    public SimpleUriProvider(String uri) {
        this.uri = URI.create(checkNotNull(uri));
    }

    @Override
    public URI getUri() {
        return uri;
    }
}
