package com.opower.rest.client.generator.core;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.opower.rest.client.generator.util.CaseInsensitiveMap;
import com.opower.rest.client.generator.util.GenericType;
import com.opower.rest.client.generator.util.HttpHeaderNames;
import com.opower.rest.client.generator.util.HttpResponseCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

/**
 * Base class for ClientResponses.
 * @param <T> the type of the entity on the response
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("unchecked")
public class BaseClientResponse<T> extends ClientResponse<T> {

    private static final Logger LOG = LoggerFactory.getLogger(BaseClientResponse.class);
    protected Providers providers;
    protected String attributeExceptionsTo;
    protected MultivaluedMap<String, String> headers;
    protected String alternateMediaType;
    protected Class<?> returnType;
    protected Type genericReturnType;
    protected Annotation[] annotations = {};
    protected int status;
    protected boolean wasReleased;
    protected Object unmarshaledEntity;
    // These can only be set by an interceptor
    protected Exception exception;
    protected BaseClientResponseStreamFactory streamFactory;
    protected ClientExecutor executor;

    private static final int BAD_REQUEST = 400;
    private static final int NETWORK_CONNECT_TIMEOUT = 599;


    /**
     * Create an instance with the given StreamFactory and ClientExecutor.
     * @param streamFactory the StreamFactory to use
     * @param executor the ClientExecutor to use
     */
    public BaseClientResponse(BaseClientResponseStreamFactory streamFactory, ClientExecutor executor) {
        this.streamFactory = streamFactory;
        this.executor = executor;
    }

    /**
     * Create an instance with the given StreamFactory.
     * @param streamFactory the StreamFactory to use
     */
    public BaseClientResponse(BaseClientResponseStreamFactory streamFactory) {
        this.streamFactory = streamFactory;
    }

    /**
     * Store entity within a byte array input stream because we want to release the connection
     * if a ClientResponseFailure is thrown.  Copy status and headers, but ignore
     * all type information stored in the ClientResponse.
     *
     * @param copy the ClientResponse to copy
     * @return the copy of the ClientResponse without the type info
     */
    public static ClientResponse copyFromError(ClientResponse copy) {
        BaseClientResponse base = (BaseClientResponse) copy;
        InputStream is = null;
        if (copy.getHeaders().containsKey(HttpHeaderNames.CONTENT_TYPE)) {
            try {
                is = base.streamFactory.getInputStream();
                byte[] bytes = ByteStreams.toByteArray(is);
                is = new ByteArrayInputStream(bytes);
            } catch (IOException e) {
                LOG.warn("unable to get headers from copy of client response because of ", e);
            }
        }
        final InputStream theIs = is;
        BaseClientResponse tmp = new BaseClientResponse(new BaseClientResponseStreamFactory() {
            InputStream stream;

            public InputStream getInputStream() throws IOException {
                return theIs;
            }

            public void performReleaseConnection() {
            }
        });
        tmp.executor = base.executor;
        tmp.status = base.status;
        tmp.providers = base.providers;
        tmp.headers = new CaseInsensitiveMap<String>();
        tmp.headers.putAll(base.headers);
        return tmp;


    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }

    public void setProviders(Providers providers) {
        this.providers = providers;
    }

    public void setReturnType(Class<T> returnType) {
        this.returnType = returnType;
    }

    public void setGenericReturnType(Type genericReturnType) {
        this.genericReturnType = genericReturnType;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public void setAttributeExceptionsTo(String attributeExceptionsTo) {
        this.attributeExceptionsTo = attributeExceptionsTo;
    }

    public Exception getException() {
        return this.exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public Annotation[] getAnnotations() {
        return this.annotations;
    }

    /**
     * Get the value of the Response header for the specified key.
     * @param headerKey the header to get
     * @return The header for the specified key
     */
    public String getResponseHeader(String headerKey) {
        if (this.headers == null) {
            return null;
        }
        return this.headers.getFirst(headerKey);
    }

    public void setAlternateMediaType(String alternateMediaType) {
        this.alternateMediaType = alternateMediaType;
    }

    public BaseClientResponseStreamFactory getStreamFactory() {
        return this.streamFactory;
    }

    @Override
    public void resetStream() {
        try {
            this.streamFactory.getInputStream().reset();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T getEntity() {
        if (this.returnType == null) {
            throw new RuntimeException(
                    "No type information to extract entity with, use other getEntity() methods");
        }
        return (T) getEntity(this.returnType, this.genericReturnType, this.annotations);
    }

    @Override
    public <T2> T2 getEntity(Class<T2> type) {
        return getEntity(type, null);
    }

    @Override
    public <T2> T2 getEntity(Class<T2> type, Type genericType) {
        return getEntity(type, genericType, getAnnotations(type, genericType));
    }

    private <T2> Annotation[] getAnnotations(Class<T2> type, Type genericType) {
        return (this.returnType == type && this.genericReturnType == genericType) ? this.annotations
                : null;
    }

    @Override
    public <T2> T2 getEntity(Class<T2> type, Type genericType, Annotation[] anns) {
        if (this.exception != null) {
            throw new RuntimeException("Unable to unmarshall response for "
                    + this.attributeExceptionsTo, this.exception);
        }

        if (this.unmarshaledEntity != null && !type.isInstance(this.unmarshaledEntity)) {
            throw new RuntimeException("The entity was already read, and it was of type "
                    + this.unmarshaledEntity.getClass());
        }

        if (this.unmarshaledEntity == null) {
            if (this.status == HttpResponseCodes.SC_NO_CONTENT) {
                return null;
            }

            this.unmarshaledEntity = readFrom(type, genericType, getMediaType(), anns);
            // only release connection if we actually unmarshalled something and if the object is *NOT* an InputStream
            // If it is an input stream, the user may be doing their own stream processing.
            if (this.unmarshaledEntity != null && !InputStream.class.isInstance(this.unmarshaledEntity)) {
                releaseConnection();
            }
        }
        return (T2) this.unmarshaledEntity;
    }

    /**
     * Get the MediaType from the Response headers.
     * @return the MediaType as found in the Response headers
     */
    protected MediaType getMediaType() {
        String mediaType = getResponseHeader(HttpHeaderNames.CONTENT_TYPE);
        if (mediaType == null) {
            mediaType = this.alternateMediaType;
        }

        return mediaType == null ? MediaType.WILDCARD_TYPE : MediaType.valueOf(mediaType);
    }

    /**
     * Read the Response returning the specified type.
     * @param type The type to return
     * @param genericType The generic type info if needed
     * @param media The MediaType to use
     * @param annotations The relevant annotations for the associated request
     * @param <T2> The type of the object that will be returned
     * @return The response converted to the appropriate type
     */
    protected <T2> Object readFrom(Class<T2> type, Type genericType,
                                   MediaType media, Annotation[] annotations) {
        Type useGeneric = genericType == null ? type : genericType;
        Class<?> useType = type;


        MessageBodyReader reader1 = this.providers.getMessageBodyReader(useType,
                useGeneric, this.annotations, media);
        if (reader1 == null) {
            throw createResponseFailure(String.format(
                    "Unable to find a MessageBodyReader of content-type %s and type %s",
                    media, genericType));
        }

        try {
            InputStream is = this.streamFactory.getInputStream();
            if (is == null) {
                throw new ClientResponseFailure("Input stream was empty, there is no entity", this);
            }

            return reader1.readFrom(useType, useGeneric, this.annotations, media, getHeaders(), is);


        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public <T2> T2 getEntity(GenericType<T2> genericType) {
        return getEntity(genericType.getType(), genericType.getGenericType());
    }

    @Override
    public <T2> T2 getEntity(GenericType<T2> genericType, Annotation[] ann) {
        return getEntity(genericType.getType(), genericType.getGenericType(), ann);
    }

    public MultivaluedMap<String, String> getHeaders() {
        return this.headers;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        // hack to cast from <String, String> to <String, Object>
        return (MultivaluedMap) this.headers;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    /**
     * Check the status code of the response to see if it falls in the range of failures.
     */
    public void checkFailureStatus() {
        if (this.status >= BAD_REQUEST && this.status <= NETWORK_CONNECT_TIMEOUT) {
            throw createResponseFailure(String.format("Error status %d %s returned", this.status, getResponseStatus()));
        }
    }

    public ClientResponseFailure createResponseFailure(String message) {
        return createResponseFailure(message, null);
    }

    public ClientResponseFailure createResponseFailure(String message, Exception e) {
        setException(e);
        this.returnType = byte[].class;
        this.genericReturnType = null;
        this.annotations = null;
        return new ClientResponseFailure(message, e, this);
    }

    @Override
    public Status getResponseStatus() {
        return Status.fromStatusCode(getStatus());
    }

    public final void releaseConnection() {
        if (!wasReleased) {
            if (streamFactory != null) streamFactory.performReleaseConnection();
            wasReleased = true;
        }
    }

    @Override
    protected final void finalize() throws Throwable {
        releaseConnection();
    }

    /**
     * Factory for managing the InputStream from Responses.
     */
    public interface BaseClientResponseStreamFactory {
        /**
         * Get the InputStream from the Response. The closing of the stream will be carefully managed.
         * @return the InputStream from the Response.
         * @throws IOException related to the InputStream
         */
        InputStream getInputStream() throws IOException;

        /**
         * Release the connection associated with the Response.
         */
        void performReleaseConnection();
    }


}
