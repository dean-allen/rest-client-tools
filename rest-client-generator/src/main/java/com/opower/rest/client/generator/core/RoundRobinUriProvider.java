package com.opower.rest.client.generator.core;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * UriProvider that provides rudimentary round-robin load balancing among the specified list of URIs.
 * @author chris.phillips
 */
public class RoundRobinUriProvider implements UriProvider{

    private final Iterator<URI> uriCycleIterator;

    /**
     * Convenience for creating the SimpleUriProvider with one or more URI Strings.
     * @param uris the Strings representing the URIs to use
     */
    public RoundRobinUriProvider(String ... uris) {
        this(Iterables.transform(Arrays.asList(checkNotNull(uris)), new Function<String, URI>() {
            @Override
            public URI apply(String input) {
                return URI.create(checkNotNull(input));
            }
        }));
    }

    /**
     * Creates a UriProvider that will round robin between the provided URI(s).
     * @param uris the URIs to use.
     */
    public RoundRobinUriProvider(URI ... uris) {
        this(Arrays.asList(checkNotNull(uris)));
    }

    /**
     * Creates a UriProvider that will round robin between the URIs contained in the Iterable
     * @param uris the uris to use.
     */
    public RoundRobinUriProvider(Iterable<URI> uris) {
        List<URI> uriList = ImmutableList.copyOf(checkNotNull(uris));
        this.uriCycleIterator = Iterators.cycle(uriList);
    }

    @Override
    public synchronized URI getUri() {
        return this.uriCycleIterator.next();
    }
}
