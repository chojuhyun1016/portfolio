package org.example.order.domain.order.repository;

import org.example.order.domain.order.model.OrderView;

import java.util.Optional;

/**
 * 주문 조회 리포지토리 (도메인 전용)
 */
public interface OrderQueryRepository {

    /**
     * 주문 ID로 단건 프로젝션을 조회한다.
     *
     * @param orderId 주문 ID
     * @return Optional<OrderView> (없으면 empty)
     */
    Optional<OrderView> fetchByOrderId(Long orderId);
}
