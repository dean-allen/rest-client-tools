package com.opower.rest.client.generator.util;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.io.Serializable;
import java.util.Comparator;

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

    public static float getQWithParamInfo(MediaType type)
    {
        if (type.getParameters() != null)
        {
            String val = type.getParameters().get("q");
            try
            {
                if (val != null)
                {
                    float rtn = Float.valueOf(val);
                    if (rtn > 1.0F)
                        throw new IllegalArgumentException("MediaType q value cannot be greater than 1.0: " + type.toString());
                    return rtn;
                }
            }
            catch (NumberFormatException e)
            {
                throw new RuntimeException("MediaType q parameter must be a float: " + type, e);
            }
        }
        return 2.0f;
    }

    public static int compareWeight(MediaType one, MediaType two)
    {
        return new MediaTypeComparator().compare(one, two);
    }

    /**
     * subtypes like application/*+xml
     *
     * @param subtype
     * @return
     */
    public static boolean isCompositeWildcardSubtype(String subtype)
    {
        return subtype.startsWith("*+");
    }

    /**
     * subtypes like application/*+xml
     *
     * @param subtype
     * @return
     */
    public static boolean isWildcardCompositeSubtype(String subtype)
    {
        return subtype.endsWith("+*");
    }

    public static boolean isComposite(String subtype)
    {
        return (isCompositeWildcardSubtype(subtype) || isWildcardCompositeSubtype(subtype));
    }

    private static class MediaTypeComparator implements Comparator<MediaType>, Serializable {

        private static final long serialVersionUID = -5828700121582498092L;

        public int compare(MediaType mediaType2, MediaType mediaType) {
            float q = getQWithParamInfo(mediaType);
            boolean wasQ = q != 2.0f;
            if (q == 2.0f) q = 1.0f;

            float q2 = getQWithParamInfo(mediaType2);
            boolean wasQ2 = q2 != 2.0f;
            if (q2 == 2.0f) q2 = 1.0f;

            if (q < q2) return -1;
            if (q > q2) return 1;

            if (mediaType.isWildcardType() && !mediaType2.isWildcardType()) return -1;
            if (!mediaType.isWildcardType() && mediaType2.isWildcardType()) return 1;
            if (mediaType.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) return -1;
            if (!mediaType.isWildcardSubtype() && mediaType2.isWildcardSubtype()) return 1;
            if (isComposite(mediaType.getSubtype()) && !isComposite(mediaType2.getSubtype()))
                return -1;
            if (!isComposite(mediaType.getSubtype()) && isComposite(mediaType2.getSubtype()))
                return 1;
            if (isCompositeWildcardSubtype(mediaType.getSubtype()) && !isCompositeWildcardSubtype(mediaType2.getSubtype()))
                return -1;
            if (!isCompositeWildcardSubtype(mediaType.getSubtype()) && isCompositeWildcardSubtype(mediaType2.getSubtype()))
                return 1;
            if (isWildcardCompositeSubtype(mediaType.getSubtype()) && !isWildcardCompositeSubtype(mediaType2.getSubtype()))
                return -1;
            if (!isWildcardCompositeSubtype(mediaType.getSubtype()) && isWildcardCompositeSubtype(mediaType2.getSubtype()))
                return 1;

            int numNonQ = 0;
            if (mediaType.getParameters() != null) {
                numNonQ = mediaType.getParameters().size();
                if (wasQ) numNonQ--;
            }

            int numNonQ2 = 0;
            if (mediaType2.getParameters() != null) {
                numNonQ2 = mediaType2.getParameters().size();
                if (wasQ2) numNonQ2--;
            }

            if (numNonQ < numNonQ2) return -1;
            if (numNonQ > numNonQ2) return 1;

            return 0;
        }
    }
}
