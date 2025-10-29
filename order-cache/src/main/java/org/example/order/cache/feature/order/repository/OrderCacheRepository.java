package org.example.order.cache.feature.order.repository;

import org.example.order.cache.feature.order.model.OrderCacheRecord;

import java.util.Optional;

/**
 * Order Cache Repository (Port)
 * - 서비스/파사드는 이 인터페이스만 의존
 */
public interface OrderCacheRepository {

    Optional<OrderCacheRecord> get(Long orderId);

    void put(OrderCacheRecord record, Long ttlSeconds);

    void evict(Long orderId);
}
