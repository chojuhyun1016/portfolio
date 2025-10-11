package org.example.order.contract.order.http.request;

import org.example.order.contract.order.http.type.MethodType;

/**
 * 주문 이벤트 발행 요청 DTO
 * - POST /api/v1/orders/publish
 * - 예: {"orderId": 12345, "methodType": "CREATE"}
 */
public record OrderPublishRequest(
        Long orderId,
        MethodType methodType
) {
}
