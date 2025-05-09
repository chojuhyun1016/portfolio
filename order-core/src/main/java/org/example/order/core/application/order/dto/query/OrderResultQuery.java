package org.example.order.core.application.order.dto.query;

import org.example.order.core.application.order.dto.model.OrderEntityModel;

/**
 * 주문 조회 결과 DTO (Application 계층)
 *
 * Query 계층에서 사용:
 * - 복합적인 조회 결과를 감싸는 DTO (ex: 단일 주문 조회 결과)
 * - OrderEntityModel → OrderItemQuery로 변환 후 래핑
 */
public record OrderResultQuery(
        OrderItemQuery order
) {
    /**
     * EntityModel → Query DTO 변환 메서드
     *
     * @param vo OrderEntityModel
     * @return OrderResultQuery DTO
     */
    public static OrderResultQuery toDto(OrderEntityModel vo) {
        OrderItemQuery orderDto = OrderItemQuery.toDto(vo.getOrder());
        return new OrderResultQuery(orderDto);
    }
}
