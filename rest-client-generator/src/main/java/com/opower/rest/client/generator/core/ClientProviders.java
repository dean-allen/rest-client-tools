package com.opower.rest.client.generator.core;

import com.opower.rest.client.generator.plugins.providers.Builtin;
import com.opower.rest.client.generator.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;

/**
 * @author chris.phillips
 */
public class ClientProviders implements Providers {

    protected MediaTypeMap<SortedKey<MessageBodyReader>> messageBodyReaders = new MediaTypeMap<>();
    protected MediaTypeMap<SortedKey<MessageBodyWriter>> messageBodyWriters = new MediaTypeMap<>();


    public ClientProviders() {
        // register the builtins
        for(Object p : Builtin.providerInstances()) {
            registerProviderInstance(p, true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType) {
        List<SortedKey<MessageBodyReader>> readers = messageBodyReaders.getPossible(mediaType, type);

        for (SortedKey<MessageBodyReader> reader : readers)
        {
            if (reader.obj.isReadable(type, genericType, annotations, mediaType))
            {
                return (MessageBodyReader<T>) reader.obj;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type,
                                                         Type genericType,
                                                         Annotation[] annotations,
                                                         MediaType mediaType) {
        List<SortedKey<MessageBodyWriter>> writers = messageBodyWriters.getPossible(mediaType, type);
        for (SortedKey<MessageBodyWriter> writer : writers)
        {
            if (writer.obj.isWriteable(type, genericType, annotations, mediaType))
            {
                return (MessageBodyWriter<T>) writer.obj;
            }
        }
        return null;
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        return null;
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return null;
    }

    /**
     * Allow us to sort message body implementations that are more specific for their types
     * i.e. MessageBodyWriter<Object> is less specific than MessageBodyWriter<String>.
     * <p/>
     * This helps out a lot when the desired media type is a wildcard and to weed out all the possible
     * default mappings.
     */
    protected static class SortedKey<T> implements Comparable<SortedKey<T>>, MediaTypeMap.Typed {
        public Class readerClass;
        public T obj;

        public boolean isGeneric = false;

        public boolean isBuiltin = false;

        public Class template = null;

        private SortedKey(Class intf, T reader, Class readerClass, boolean isBuiltin) {
            this(intf, reader, readerClass);
            this.isBuiltin = isBuiltin;
        }

        private SortedKey(Class intf, T reader, Class readerClass) {
            this.readerClass = readerClass;
            this.obj = reader;
            // check the super class for the generic type 1st
            template = Types.getTemplateParameterOfInterface(readerClass, intf);
            isGeneric = template == null || Object.class.equals(template);
        }

        public int compareTo(SortedKey<T> tMessageBodyKey) {
            // Sort more specific template parameter types before non-specific
            // Sort user provider before builtins
            if (this == tMessageBodyKey) {
                return 0;
            }
            if (isGeneric != tMessageBodyKey.isGeneric) {
                if (isGeneric) {
                    return 1;
                } else {
                    return -1;
                }
            }
            if (isBuiltin == tMessageBodyKey.isBuiltin) {
                return 0;
            }
            if (isBuiltin) {
                return 1;
            } else {
                return -1;
            }
        }

        public Class getType() {
            return template;
        }
    }

    /**
     * Register a @Provider class.  Can be a MessageBodyReader/Writer or ExceptionMapper.
     *
     * @param provider
     */
    public void registerProviderInstance(Object provider) {
        registerProviderInstance(provider, false);
    }

    public void registerProviderInstance(Object provider, boolean builtin) {
        if (provider instanceof MessageBodyReader) {
            try {
                addMessageBodyReader((MessageBodyReader)provider, provider.getClass(), builtin);
            } catch (Exception e) {
                throw new RuntimeException("Unable to register MessageBodyReader", e);
            }
        }
        if (provider instanceof MessageBodyWriter) {
            try {
                addMessageBodyWriter((MessageBodyWriter)provider, provider.getClass(), builtin);
            } catch (Exception e) {
                throw new RuntimeException("Unable to register MessageBodyWriter", e);
            }
        }
    }

    /**
     * Specify the provider class.  This is there just in case the provider instance is a proxy.  Proxies tend
     * to lose generic type information
     *
     * @param provider
     * @param providerClass
     * @param isBuiltin
     */
    public void addMessageBodyReader(MessageBodyReader provider, Class providerClass, boolean isBuiltin)
    {
        SortedKey<MessageBodyReader> key = new SortedKey<>(MessageBodyReader.class, provider, providerClass, isBuiltin);
        Consumes consumeMime = provider.getClass().getAnnotation(Consumes.class);
        if (consumeMime != null)
        {
            for (String consume : consumeMime.value())
            {
                MediaType mime = MediaType.valueOf(consume);
                messageBodyReaders.add(mime, key);
            }
        }
        else
        {
            messageBodyReaders.add(new MediaType("*", "*"), key);
        }
    }

    /**
     * Specify the provider class.  This is there jsut in case the provider instance is a proxy.  Proxies tend
     * to lose generic type information
     *
     * @param provider
     * @param providerClass
     * @param isBuiltin
     */
    public void addMessageBodyWriter(MessageBodyWriter provider, Class providerClass, boolean isBuiltin)
    {
        Produces consumeMime = provider.getClass().getAnnotation(Produces.class);
        SortedKey<MessageBodyWriter> key = new SortedKey<>(MessageBodyWriter.class, provider, providerClass, isBuiltin);
        if (consumeMime != null)
        {
            for (String consume : consumeMime.value())
            {
                MediaType mime = MediaType.valueOf(consume);
                messageBodyWriters.add(mime, key);
            }
        }
        else
        {
            messageBodyWriters.add(new MediaType("*", "*"), key);
        }
    }
}
