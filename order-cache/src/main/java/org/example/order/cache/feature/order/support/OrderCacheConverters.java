package org.example.order.cache.feature.order.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.order.cache.feature.order.model.OrderCacheRecord;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 캐시 역직렬화/변환 유틸
 * - Redis 값 유형이 Map / JSON 문자열 / 객체 등 혼재될 때, 공통 변환 지점
 * - 운영 환경에서 스키마 Drift를 흡수하기 위한 방어적 매핑
 */
public final class OrderCacheConverters {

    private OrderCacheConverters() {
    }

    public static OrderCacheRecord fromMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return new OrderCacheRecord(
                asLong(map.get("orderId")),
                asStr(map.get("orderNumber")),
                asLong(map.get("userId")),
                asStr(map.get("userNumber")),
                asLong(map.get("orderPrice")),
                asBool(map.get("deleteYn")),
                asLong(map.get("createdUserId")),
                asStr(map.get("createdUserType")),
                asLdt(map.get("createdDatetime")),
                asLong(map.get("modifiedUserId")),
                asStr(map.get("modifiedUserType")),
                asLdt(map.get("modifiedDatetime")),
                asLong(map.get("publishedTimestamp")),
                asLong(map.get("versionStamp"))
        );
    }

    public static OrderCacheRecord fromJson(ObjectMapper om, String json) {
        try {
            Map<String, Object> m = om.readValue(json, new TypeReference<Map<String, Object>>() {
            });

            return fromMap(m);
        } catch (Exception e) {
            return null;
        }
    }

    private static String asStr(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }

        if (v instanceof Number n) {
            return n.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Boolean asBool(Object v) {
        if (v == null) {
            return null;
        }

        if (v instanceof Boolean b) {
            return b;
        }

        String s = String.valueOf(v);

        if ("Y".equalsIgnoreCase(s) || "YES".equalsIgnoreCase(s) || "TRUE".equalsIgnoreCase(s)) {
            return Boolean.TRUE;
        }

        if ("N".equalsIgnoreCase(s) || "NO".equalsIgnoreCase(s) || "FALSE".equalsIgnoreCase(s)) {
            return Boolean.FALSE;
        }

        try {
            return Integer.parseInt(s) != 0;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDateTime asLdt(Object v) {
        if (v == null) {
            return null;
        }

        if (v instanceof LocalDateTime ldt) {
            return ldt;
        }

        try {
            return LocalDateTime.parse(String.valueOf(v));
        } catch (Exception ignored) {
            return null;
        }
    }
}
