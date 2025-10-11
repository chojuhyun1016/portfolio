package org.example.order.contract.order.messaging.payload;

/**
 * 메시지 페이로드 (주문 기본 정보)
 * - 필요 필드만 노출
 */
public record OrderPayload(
        Long orderId,
        String orderNumber,
        Long userId,
        String userNumber,
        Long orderPrice
) {
}
