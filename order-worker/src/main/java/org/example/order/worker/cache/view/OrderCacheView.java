package org.example.order.worker.cache.view;

import org.example.order.core.application.order.dto.sync.LocalOrderSync;

import java.io.Serializable;

/**
 * Redis 캐시 전용 View
 * - 조회/키 인덱싱에 필요한 필드만 유지
 * - 동기화 DTO(LocalOrderSync)와 분리하여 캐시 스키마 안정성 확보
 */
public record OrderCacheView(
        Long orderId,
        String orderNumber,
        Long userId,
        String userNumber,
        Long orderPrice,
        long versionStamp
) implements Serializable {

    public static OrderCacheView of(LocalOrderSync d) {
        if (d == null) {
            return null;
        }

        long vs = (d.version() == null ? 0L : d.version());

        return new OrderCacheView(
                d.orderId(),
                d.orderNumber(),
                d.userId(),
                d.userNumber(),
                d.orderPrice(),
                vs
        );
    }
}
