package com.opower.rest.client.generator.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;

/**
 * @author chris.phillips
 */
public class StringConverters {

    private static ConcurrentMap<Class<?>, StringConverter<?>> CONVERTERS;

    public static <T> StringConverter<T> getStringConverter(Class<T> clazz) {
        if(CONVERTERS == null) {
            synchronized (StringConverters.class) {
                if(CONVERTERS == null) {
                    CONVERTERS = init();
                }
            }
        }
        return (StringConverter<T>)CONVERTERS.get(clazz);
    }

    private static ConcurrentMap<Class<?>, StringConverter<?>> init() {
        ConcurrentMap<Class<?>, StringConverter<?>> map = new MapMaker().makeMap();
        map.putAll(ImmutableMap.<Class<?>, StringConverter<?>>builder()
                .put(Optional.absent().getClass(), new OptionalConverter())
                .put(Optional.of(true).getClass(), new OptionalConverter())
                .put(Cookie.class, new CookieStringConverter())
                .put(EntityTag.class, new EntityTagDelegate())
                .put(Locale.class, new LocaleDelegate())
                .put(MediaType.class, new MediaTypeStringConverter())
                .put(URI.class, new UriStringConverter())
                .put(NewCookie.class, new NewCookieStringConverter())
                .put(CacheControl.class, new CacheControlDelegate())
                .build());
        return map;
    }
    private static abstract class OneWayStringConverter<T> implements StringConverter<T> {
        @Override
        public T fromString(String string) {
            throw new UnsupportedOperationException();
        }
    }

    public static class OptionalConverter extends OneWayStringConverter<Optional<?>> {
        @Override
        public String toString(Optional<?> value) {
            if (value.isPresent()) {
                return value.get().toString();
            }

            return "";
        }
    }

    public static class CookieStringConverter extends OneWayStringConverter<Cookie> {
        public String toString(Cookie cookie) {
            StringBuffer buf = new StringBuffer();
            ServerCookie.appendCookieValue(buf, 0, cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), null, -1, false);
            return buf.toString();
        }
    }

    public static class EntityTagDelegate extends OneWayStringConverter<EntityTag> {

        public String toString(EntityTag value) {
            String weak = value.isWeak() ? "W/" : "";
            return weak + value.getValue();
        }

    }

    public static class LocaleDelegate extends OneWayStringConverter<Locale> {



        public String toString(Locale value) {
            return toLanguageString(value);
        }

        /**
         * HTTP 1.1 has different String format for language than what java.util.Locale does '-' instead of '_'
         * as a separator
         *
         * @param value
         * @return
         */
        private String toLanguageString(Locale value) {
            StringBuffer buf = new StringBuffer(value.getLanguage().toLowerCase());
            if (value.getCountry() != null && !value.getCountry().equals(""))
                buf.append("-").append(value.getCountry().toLowerCase());
            return buf.toString();
        }
    }

    public static class MediaTypeStringConverter implements StringConverter<MediaType> {

        @Override
        public MediaType fromString(String type) {
            if (type == null) throw new IllegalArgumentException("MediaType value is null");
            return parse(type);
        }


        public static MediaType parse(String type)
        {
            String params = null;
            int idx = type.indexOf(";");
            if (idx > -1)
            {
                params = type.substring(idx + 1).trim();
                type = type.substring(0, idx);
            }
            String major = null;
            String subtype = null;
            String[] paths = type.split("/");
            if (paths.length < 2 && type.equals("*"))
            {
                major = "*";
                subtype = "*";

            }
            else if (paths.length != 2)
            {
                throw new IllegalArgumentException("Failure parsing MediaType string: " + type);
            }
            else if (paths.length == 2)
            {
                major = paths[0];
                subtype = paths[1];
            }
            if (params != null && !params.equals(""))
            {
                HashMap<String, String> typeParams = new HashMap<String, String>();

                int start = 0;

                while (start < params.length())
                {
                    start = setParam(typeParams, params, start);
                }
                return new MediaType(major, subtype, typeParams);
            }
            else
            {
                return new MediaType(major, subtype);
            }
        }

        static int getEndName(String params, int start)
        {
            int equals = params.indexOf('=', start);
            int semicolon = params.indexOf(';', start);
            if (equals == -1 && semicolon == -1) return params.length();
            if (equals == -1) return semicolon;
            if (semicolon == -1) return equals;
            int end = (equals < semicolon) ? equals : semicolon;
            return end;
        }

        public static int setParam(HashMap<String, String> typeParams, String params, int start)
        {
            boolean quote = false;
            boolean backslash = false;

            int end = getEndName(params, start);
            String name = params.substring(start, end).trim();
            if (end < params.length() && params.charAt(end) == '=') end++;

            StringBuilder buffer = new StringBuilder(params.length() - end);
            int i = end;
            for (; i < params.length(); i++)
            {
                char c = params.charAt(i);

                switch (c)
                {
                    case '"':
                    {
                        if (backslash)
                        {
                            backslash = false;
                            buffer.append(c);
                        }
                        else
                        {
                            quote = !quote;
                        }
                        break;
                    }
                    case '\\':
                    {
                        if (backslash)
                        {
                            backslash = false;
                            buffer.append(c);
                        }
                        break;
                    }
                    case ';':
                    {
                        if (!quote)
                        {
                            String value = buffer.toString().trim();
                            typeParams.put(name, value);
                            return i + 1;
                        }
                        else
                        {
                            buffer.append(c);
                        }
                        break;
                    }
                    default:
                    {
                        buffer.append(c);
                        break;
                    }
                }
            }
            String value = buffer.toString().trim();
            typeParams.put(name, value);
            return i;
        }

        public String toString(MediaType type) {
            String rtn = type.getType().toLowerCase() + "/" + type.getSubtype().toLowerCase();
            if (type.getParameters() == null || type.getParameters().size() == 0) return rtn;
            for (String name : type.getParameters().keySet()) {
                String val = type.getParameters().get(name);
                rtn += ";" + name + "=\"" + val + "\"";
            }
            return rtn;
        }
    }

    public static class UriStringConverter extends OneWayStringConverter<URI> {

        public String toString(URI uri) {
            return uri.toASCIIString();
        }
    }

    public static class NewCookieStringConverter extends OneWayStringConverter<NewCookie> {

        protected void quote(StringBuilder b, String value) {
            if (value.indexOf(' ') > -1) {
                b.append('"');
                b.append(value);
                b.append('"');
            } else {
                b.append(value);
            }
        }

        public String toString(NewCookie cookie) {
            StringBuilder b = new StringBuilder();

            b.append(cookie.getName()).append('=');
            quote(b, cookie.getValue());

            b.append(";").append("Version=").append(cookie.getVersion());

            if (cookie.getComment() != null) {
                b.append(";Comment=");
                quote(b, cookie.getComment());
            }
            if (cookie.getDomain() != null) {
                b.append(";Domain=");
                quote(b, cookie.getDomain());
            }
            if (cookie.getPath() != null) {
                b.append(";Path=");
                quote(b, cookie.getPath());
            }
            if (cookie.getMaxAge() != -1) {
                b.append(";Max-Age=");
                b.append(cookie.getMaxAge());
            }
            if (cookie.isSecure())
                b.append(";Secure");
            return b.toString();
        }
    }

    public static class CacheControlDelegate extends OneWayStringConverter<CacheControl> {

        private StringBuffer addDirective(String directive, StringBuffer buffer) {
            if (buffer.length() > 0) buffer.append(", ");
            buffer.append(directive);
            return buffer;
        }

        public String toString(CacheControl value) {
            StringBuffer buffer = new StringBuffer();
            if (value.isNoCache()) {
                List<String> fields = value.getNoCacheFields();
                if (fields.size() < 1) {
                    addDirective("no-cache", buffer);
                } else {
                    for (String field : value.getNoCacheFields()) {
                        addDirective("no-cache", buffer).append("=\"").append(field).append("\"");
                    }
                }
            }
            if (value.isMustRevalidate()) addDirective("must-revalidate", buffer);
            if (value.isNoTransform()) addDirective("no-transform", buffer);
            if (value.isNoStore()) addDirective("no-store", buffer);
            if (value.isProxyRevalidate()) addDirective("rest-revalidate", buffer);
            if (value.getSMaxAge() > -1) addDirective("s-maxage", buffer).append("=").append(value.getSMaxAge());
            if (value.getMaxAge() > -1) addDirective("max-age", buffer).append("=").append(value.getMaxAge());
            if (value.isPrivate()) {
                List<String> fields = value.getPrivateFields();
                if (fields.size() < 1) addDirective("private", buffer);
                else {
                    for (String field : value.getPrivateFields()) {
                        addDirective("private", buffer).append("=\"").append(field).append("\"");
                    }
                }
            }
            for (String key : value.getCacheExtension().keySet()) {
                String val = value.getCacheExtension().get(key);
                addDirective(key, buffer);
                if (val != null && !"".equals(val)) {
                    buffer.append("=\"").append(val).append("\"");
                }
            }
            return buffer.toString();
        }

    }
}
