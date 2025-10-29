package org.example.order.cache.feature.order.key;

/**
 * 캐시 키 규약/버전
 * - 네임스페이스 및 스키마 버전은 이곳에서만 관리
 */
public final class OrderCacheKeys {

    private static final String V = "v1";

    private OrderCacheKeys() {
    }

    /**
     * 주문 단건 캐시 키: order:v1:order:{orderId}
     */
    public static String orderKey(Long orderId) {
        return "order:" + V + ":order:" + orderId;
    }
}
