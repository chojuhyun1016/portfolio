package org.example.order.contract.order.http.response;

/**
 * 주문 단건 조회 응답 래퍼
 * - 예: {"order": { ...OrderDetail... }}
 */
public record OrderGetResponse(OrderDetail order) {
}
