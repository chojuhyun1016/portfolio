package org.example.order.common.support.messaging;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * KafkaHeaderUtils
 * - Headers -> Map<String,String> 변환
 * - retry-count 추출 유틸 (Headers / Map 양쪽 지원)
 */
public final class KafkaHeaderUtils {

    private KafkaHeaderUtils() {
    }

    public static Map<String, String> toStringMap(Headers headers) {
        Map<String, String> map = new HashMap<>();

        if (headers == null) {
            return map;
        }

        headers.forEach(h -> {
            String key = h.key();
            byte[] value = h.value();

            if (value != null) {
                map.put(key, new String(value, StandardCharsets.UTF_8));
            }
        });

        return map;
    }

    public static int getRetryCount(Headers headers, int defaultValue) {
        if (headers == null) {
            return defaultValue;
        }

        String[] keys = {"retryCount", "retry-count", "X-Retry-Count", "x-retry-count"};

        for (String k : keys) {
            Header h = headers.lastHeader(k);

            if (h != null && h.value() != null) {
                try {
                    return Integer.parseInt(new String(h.value(), StandardCharsets.UTF_8).trim());
                } catch (NumberFormatException ignore) {
                }
            }
        }

        return defaultValue;
    }

    public static int getRetryCountFromMap(Map<String, String> headers, int defaultValue) {
        if (headers == null) {
            return defaultValue;
        }

        String[] keys = {"retryCount", "retry-count", "X-Retry-Count", "x-retry-count"};

        for (String k : keys) {
            String v = headers.get(k);

            if (v != null) {
                try {
                    return Integer.parseInt(v.trim());
                } catch (NumberFormatException ignore) {
                }
            }
        }

        return defaultValue;
    }
}
