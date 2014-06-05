package com.opower.rest.client.generator.marshallers;


import com.opower.rest.client.generator.core.ClientRequest;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PathParamMarshaller implements Marshaller {
    private String paramName;

    public PathParamMarshaller(String paramName) {
        this.paramName = paramName;
    }

    public void build(ClientRequest request, Object object) {
        request.pathParameter(paramName, object);
    }

}
