package org.example.order.core.application.order.dto.query;

/**
 * 주문 단건 조회용 Query DTO (Application 계층 전용)
 * - Controller 요청값을 매퍼가 변환하여 서비스로 전달
 */
public record OrderQuery(Long orderId) {
}
