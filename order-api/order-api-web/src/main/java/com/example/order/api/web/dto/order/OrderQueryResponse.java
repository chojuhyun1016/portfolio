package com.example.order.api.web.dto.order;

import java.time.LocalDateTime;

/**
 * 주문 단건 조회 응답 DTO (API 레이어)
 * - Application의 OrderView에서 필요한 값을 투영
 */
public record OrderQueryResponse(
        Long id,
        Long userId,
        String userNumber,
        Long orderId,
        String orderNumber,
        Long orderPrice,
        Boolean deleteYn,
        Long version,
        Long createdUserId,
        String createdUserType,
        LocalDateTime createdDatetime,
        Long modifiedUserId,
        String modifiedUserType,
        LocalDateTime modifiedDatetime,
        Long publishedTimestamp,
        Boolean failure
) {
}
