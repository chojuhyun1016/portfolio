package com.example.order.api.web.dto.order;

/**
 * 주문 단건 조회 응답 DTO (API 레이어)
 * - Application의 LocalOrderDto에서 필요한 값만 투영
 */
public record OrderResponse(
        Long id,
        Long userId,
        String userNumber,
        Long orderId,
        String orderNumber,
        Long orderPrice,
        Boolean deleteYn,
        Long version,
        Long publishedTimestamp
) {
}
