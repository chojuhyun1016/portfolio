package org.example.order.core.application.order.mapper;

import org.example.order.cache.feature.order.model.OrderCacheRecord;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;

/**
 * OrderCacheAssembler
 * ------------------------------------------------------------------------
 * 목적
 * - LocalOrderSync → OrderCacheRecord (캐시 쓰기용) 변환
 * - 캐시 스키마에 필요한 최소 항목만 채움
 */
public final class OrderCacheAssembler {

    private OrderCacheAssembler() {
    }

    public static OrderCacheRecord from(LocalOrderSync d) {
        if (d == null) {
            return null;
        }

        return new OrderCacheRecord(
                d.orderId(),
                d.orderNumber(),
                d.userId(),
                d.userNumber(),
                d.orderPrice(),
                d.deleteYn(),
                d.createdUserId(),
                d.createdUserType(),
                d.createdDatetime(),
                d.modifiedUserId(),
                d.modifiedUserType(),
                d.modifiedDatetime(),
                d.publishedTimestamp(),
                d.version()
        );
    }
}
