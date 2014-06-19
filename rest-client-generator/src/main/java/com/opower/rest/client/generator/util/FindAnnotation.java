package com.opower.rest.client.generator.util;

import java.lang.annotation.Annotation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("unchecked")
public final class FindAnnotation {
    private FindAnnotation() {
    }

    /**
     * FIXME Comment this
     *
     * @param <T>
     * @param searchList
     * @param annotation
     * @return
     */
    public static <T> T findAnnotation(Annotation[] searchList, Class<T> annotation) {
        if (searchList == null) return null;
        for (Annotation ann : searchList) {
            if (ann.annotationType().equals(annotation)) {
                return (T) ann;
            }
        }
        return null;
    }
}
