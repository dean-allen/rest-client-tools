package com.opower.rest.client.generator.util;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MediaTypeHelper {
    public static MediaType getConsumes(Class<?> declaring, AccessibleObject method) {
        Consumes consume = method.getAnnotation(Consumes.class);
        if (consume == null) {
            consume = declaring.getAnnotation(Consumes.class);
            if (consume == null) return null;
        }
        return MediaType.valueOf(consume.value()[0]);
    }

    public static MediaType getProduces(Class<?> declaring, Method method) {
        return getProduces(declaring, method, MediaType.APPLICATION_JSON_TYPE);
    }

    public static MediaType getProduces(Class<?> declaring, Method method, MediaType defaultProduces) {
        Produces consume = method.getAnnotation(Produces.class);
        if (consume == null) {
            consume = declaring.getAnnotation(Produces.class);
        }
        if (consume == null) return defaultProduces;
        return MediaType.valueOf(consume.value()[0]);
    }
}
