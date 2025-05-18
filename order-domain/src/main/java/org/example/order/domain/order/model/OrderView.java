package org.example.order.domain.order.model;

/**
 * Order 조회 결과 (Domain 전용 View 모델)
 * - 외부 레이어에 의존하지 않는 순수 도메인 레코드 모델
 */
public record OrderView(
        Long orderId,
        String orderNumber,
        Long userId,
        String userNumber,
        Long orderPrice
) {}
