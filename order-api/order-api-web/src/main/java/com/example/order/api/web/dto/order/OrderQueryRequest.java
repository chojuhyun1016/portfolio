package com.example.order.api.web.dto.order;

import jakarta.validation.constraints.NotNull;

/**
 * 주문 단건 조회 요청 DTO (API 레이어)
 * - POST body 로 orderId 전달
 */
public record OrderQueryRequest(
        @NotNull(message = "orderId 는 필수입니다.")
        Long orderId
) {
}
