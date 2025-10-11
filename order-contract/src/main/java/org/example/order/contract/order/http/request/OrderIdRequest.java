package org.example.order.contract.order.http.request;

/**
 * 주문 단건 조회 요청 DTO
 * - POST /api/v1/orders/query
 * - 예: {"orderId": 12345}
 */
public record OrderIdRequest(Long orderId) {
}
