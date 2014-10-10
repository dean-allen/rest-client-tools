package com.opower.rest.client.generator.plugins.providers;

import com.google.common.collect.ImmutableSet;
import com.opower.rest.client.generator.util.StringConverter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.ws.rs.ext.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Builtin
{

    private static ConcurrentMap<Class<?>, Object> PROVIDERS;

    public static Collection<Object> providerInstances() {
        if(PROVIDERS == null) {
            synchronized (Builtin.class) {
                if(PROVIDERS == null) {
                    PROVIDERS = init();
                }
            }
        }
        return ImmutableSet.copyOf(PROVIDERS.values());
    }

    private static ConcurrentMap<Class<?>, Object> init() {
        ConcurrentMap<Class<?>, Object> providerMap = new ConcurrentHashMap<>();
        providerMap.putIfAbsent(ByteArrayProvider.class, new ByteArrayProvider());
        providerMap.putIfAbsent(DataSourceProvider.class, new DataSourceProvider());
        providerMap.putIfAbsent(DefaultTextPlain.class, new DefaultTextPlain());
        providerMap.putIfAbsent(DocumentProvider.class, new DocumentProvider());
        providerMap.putIfAbsent(FileProvider.class, new FileProvider());
        providerMap.putIfAbsent(FormUrlEncodedProvider.class, new FormUrlEncodedProvider());
        providerMap.putIfAbsent(IIOImageProvider.class, new IIOImageProvider());
        providerMap.putIfAbsent(InputStreamProvider.class, new InputStreamProvider());
        providerMap.putIfAbsent(StreamingOutputProvider.class, new StreamingOutputProvider());
        providerMap.putIfAbsent(StringTextStar.class, new StringTextStar());
        return providerMap;
    }

}
