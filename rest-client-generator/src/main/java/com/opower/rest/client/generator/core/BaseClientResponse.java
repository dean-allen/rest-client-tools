/**
 *    Copyright 2014 Opower, Inc.
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 **/
package com.opower.rest.client.generator.core;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.opower.rest.client.generator.util.CaseInsensitiveMap;
import com.opower.rest.client.generator.util.GenericType;
import com.opower.rest.client.generator.util.HttpHeaderNames;
import com.opower.rest.client.generator.util.HttpResponseCodes;
import javax.ws.rs.core.Response;
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
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 *
 */
@SuppressWarnings("unchecked")
public class BaseClientResponse extends ClientResponse {

    private static final Logger LOG = LoggerFactory.getLogger(BaseClientResponse.class);
    protected Providers providers;
    protected String attributeExceptionsTo;
    protected CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<>();
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

    private final Predicate<Integer> errorStatusCriteria;

    /**
     * Create an instance with the given StreamFactory and ClientExecutor.
     * @param streamFactory the StreamFactory to use
     * @param executor the ClientExecutor to use
     * @param errorStatusCriteria
     */
    public BaseClientResponse(BaseClientResponseStreamFactory streamFactory, ClientExecutor executor, Predicate<Integer> errorStatusCriteria) {
        this.streamFactory = streamFactory;
        this.executor = executor;
        this.errorStatusCriteria = errorStatusCriteria;
    }

    /**
     * Create an instance with the given StreamFactory.
     * @param streamFactory the StreamFactory to use
     * @param errorStatusCriteria
     */
    public BaseClientResponse(BaseClientResponseStreamFactory streamFactory, Predicate<Integer> errorStatusCriteria) {
        this.streamFactory = streamFactory;
        this.errorStatusCriteria = errorStatusCriteria;
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
            public InputStream getInputStream() throws IOException {
                return theIs;
            }

            public void performReleaseConnection() {
            }
        }, base.errorStatusCriteria);
        tmp.executor = base.executor;
        tmp.status = base.status;
        tmp.providers = base.providers;
        tmp.headers = new CaseInsensitiveMap<>();
        tmp.headers.putAll(base.headers);
        return tmp;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setHeaders(CaseInsensitiveMap<String> headers) {
        this.headers = headers;
    }

    public void setProviders(Providers providers) {
        this.providers = providers;
    }

    public void setReturnType(Class<?> returnType) {
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
    public boolean resetStream() {
        try {
            this.streamFactory.getInputStream().reset();
            return true;
        } catch (Exception e) {
            LOG.debug("couldn't reset stream.");
            return false;
        }
    }

    @Override
    public Object getEntity() {
        if (this.returnType == null) {
            throw new RuntimeException(
                    "No type information to extract entity with, use other getEntity() methods");
        }
        // this seems like the best we can do. You get the InputStream from the response.
        if (Response.class.isAssignableFrom(this.returnType)) {
            return getEntity(InputStream.class, null, this.annotations);
        } else {
            return getEntity(this.returnType, this.genericReturnType, this.annotations);
        }
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
        @SuppressWarnings("unchecked")
        T2 entityToReturn = (T2)this.unmarshaledEntity;
        return entityToReturn;
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

        if (this.errorStatusCriteria.apply(this.status)) {
            throw createResponseFailure(String.format("Error status %d %s returned", this.status, getResponseStatus()));
        }
    }

    public boolean isSuccessful() {
        return !this.errorStatusCriteria.apply(this.getStatus());
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
