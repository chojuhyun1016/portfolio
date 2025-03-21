package org.example.order.core.application.dto.order;

import org.example.order.core.application.vo.order.OrderVo;

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
