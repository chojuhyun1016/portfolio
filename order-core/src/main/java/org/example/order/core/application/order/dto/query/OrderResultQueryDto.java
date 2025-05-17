package org.example.order.core.application.order.dto.query;

import org.example.order.core.application.order.dto.model.OrderEntityModelDto;

/**
 * 주문 조회 결과 DTO (Application 계층)
 *
 * Query 계층에서 사용:
 * - 복합적인 조회 결과를 감싸는 DTO (ex: 단일 주문 조회 결과)
 * - OrderEntityModel → OrderItemQuery로 변환 후 래핑
 */
public record OrderResultQueryDto(
        OrderItemQueryDto order
) {
    /**
     * EntityModel → Query DTO 변환 메서드
     *
     * @param vo OrderEntityModel
     * @return OrderResultQuery DTO
     */
    public static OrderResultQueryDto toDto(OrderEntityModelDto vo) {
        OrderItemQueryDto orderDto = OrderItemQueryDto.toDto(vo.getOrder());
        return new OrderResultQueryDto(orderDto);
    }
}
