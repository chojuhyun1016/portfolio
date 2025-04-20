package org.example.order.core.application.order.query;

import org.example.order.core.application.order.vo.OrderVo;

/**
 * global order data
 */
public record OrderCrudDto(
        OrderCrudEntityDto order
) {
    public static OrderCrudDto toDto(OrderVo vo) {
        OrderCrudEntityDto orderDto = OrderCrudEntityDto.toDto(vo.getOrder());

        return new OrderCrudDto(orderDto);
    }
}
