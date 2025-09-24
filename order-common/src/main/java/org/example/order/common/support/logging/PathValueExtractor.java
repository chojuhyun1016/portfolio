package org.example.order.common.support.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * 프레임워크 타입에 컴파일 의존하지 않도록 리플렉션으로 경로 탐색
 */
final class PathValueExtractor {

    private PathValueExtractor() {
    }

    static String extract(Object[] args, String... paths) {
        if (paths == null || paths.length == 0) {
            return null;
        }

        for (String path : paths) {
            String p = path == null ? "" : path.trim();

            if (p.isEmpty()) {
                continue;
            }

            for (Object arg : args) {
                String val = tryRead(arg, p);

                if (val != null && !val.isBlank()) {
                    return val;
                }
            }
        }

        return null;
    }

    private static String tryRead(Object root, String path) {
        if (root == null) {
            return null;
        }

        String[] segs = path.split("\\.");
        Object cur = root;

        for (int i = 0; i < segs.length; i++) {
            String seg = segs[i];

            if ("key".equals(seg) || "value".equals(seg) || "payload".equals(seg)) {
                Object next = invokeZeroArg(cur, seg);

                if (next == null) {
                    next = invokeZeroArg(cur, "get" + cap(seg));
                }

                if (next == null) {
                    return null;
                }

                cur = next;

                continue;
            }

            if ("headers".equals(seg)) {
                String nextSeg = (i + 1 < segs.length) ? segs[i + 1] : null;
                Object headers = resolveHeaders(cur);

                if (nextSeg == null) {
                    return toStringSafe(headers);
                }

                Object v = readHeader(headers, nextSeg);

                if (v == null) {
                    return null;
                }

                cur = v;
                i++;

                continue;
            }

            cur = readProperty(cur, seg);

            if (cur == null) {
                return null;
            }
        }

        return toStringSafe(cur);
    }

    private static Object resolveHeaders(Object cur) {
        if (cur == null) {
            return null;
        }

        Object h = invokeZeroArg(cur, "getHeaders");

        if (h == null) {
            h = invokeZeroArg(cur, "headers");
        }

        return h;
    }

    private static Object readHeader(Object headers, String key) {
        if (headers == null) {
            return null;
        }

        if (headers instanceof Map<?, ?> m) {
            return m.get(key);
        }

        // Kafka Headers: lastHeader(String)->Header.value(): byte[]
        try {
            Method lastHeader = headers.getClass().getMethod("lastHeader", String.class);
            Object header = lastHeader.invoke(headers, key);

            if (header != null) {
                Method value = header.getClass().getMethod("value");
                Object bytes = value.invoke(header);

                if (bytes instanceof byte[] ba) {
                    return new String(ba, Charset.defaultCharset());
                }
            }
        } catch (Exception ignore) {
        }

        // Spring MessageHeaders: get(String)
        try {
            Method get = headers.getClass().getMethod("get", Object.class);

            return get.invoke(headers, key);
        } catch (Exception ignore) {
        }

        return null;
    }

    private static Object readProperty(Object cur, String seg) {
        if (cur == null) {
            return null;
        }

        if (cur instanceof Map<?, ?> m) {
            return m.get(seg);
        }

        Object next = invokeZeroArg(cur, "get" + cap(seg));

        if (next != null) {
            return next;
        }

        next = invokeZeroArg(cur, "is" + cap(seg));

        if (next != null) {
            return next;
        }

        next = invokeZeroArg(cur, seg);

        if (next != null) {
            return next;
        }

        try {
            Field f = cur.getClass().getDeclaredField(seg);
            f.setAccessible(true);

            return f.get(cur);
        } catch (Exception ignore) {
        }

        return null;
    }

    private static Object invokeZeroArg(Object target, String name) {
        if (target == null) {
            return null;
        }

        try {
            Method m = target.getClass().getMethod(name);

            return m.invoke(target);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String toStringSafe(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }
}
