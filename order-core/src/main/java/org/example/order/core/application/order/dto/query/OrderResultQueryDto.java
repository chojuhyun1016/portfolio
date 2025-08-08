package org.example.order.core.application.order.dto.query;

import org.example.order.core.application.order.dto.internal.OrderEntityDto;

/**
 * 주문 조회 결과 DTO (복합 응답 래퍼)
 * - 단일 주문 상세를 감싸는 형태
 */
public record OrderResultQueryDto(
        OrderDetailQueryDto order
) {
    /**
     * 내부 DTO → Query 래퍼 변환
     */
    public static OrderResultQueryDto from(OrderEntityDto dto) {
        return new OrderResultQueryDto(OrderDetailQueryDto.from(dto));
    }
}
