package org.example.order.domain.order.repository;

import org.example.order.domain.order.entity.OrderEntity;

import java.util.List;
import java.util.Optional;

/**
 * 기본 Repository 인터페이스 (도메인)
 */
public interface OrderRepository {
    Optional<OrderEntity> findById(Long id);

    void save(OrderEntity entity);

    void deleteByOrderIdIn(List<Long> orderId);
}
