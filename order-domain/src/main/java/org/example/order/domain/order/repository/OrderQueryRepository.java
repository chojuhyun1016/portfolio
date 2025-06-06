package org.example.order.domain.order.repository;

import org.example.order.domain.order.model.OrderView;

/**
 * 조회 전용 Repository 인터페이스 (도메인)
 */
public interface OrderQueryRepository {
    OrderView fetchByOrderId(Long orderId);
}
