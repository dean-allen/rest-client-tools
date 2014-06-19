package com.opower.rest.test.resource;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Meaningless implementation of FrobResource. The point of the tests is that they show the client
 * can talk correctly to the JAX-RS resource so this just makes dumb responses.
 * @author chris.phillips
 */
public class FrobServerResource implements FrobResource {

    private static final LoadingCache<String, Frob> FROBS = CacheBuilder.newBuilder().build(new CacheLoader<String, Frob>() {
        @Override
        public Frob load(String key) throws Exception {
            return new Frob(key);
        }
    });

    @Override
    public Frob findFrob(String frobId) {

        try {
            return FROBS.get(checkNotNull(frobId));
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Frob updateFrob(String frobId, String name) {

        try {
            return FROBS.get(checkNotNull(frobId));
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Response createFrob(Frob frob) {
        FROBS.put(frob.getId(), frob);
        return Response.ok().build();
    }
}
