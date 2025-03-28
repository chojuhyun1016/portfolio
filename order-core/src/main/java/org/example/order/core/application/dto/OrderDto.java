package org.example.order.core.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.core.application.vo.OrderVo;

/**
 * local order data
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDto {
    private OrderLocalDto order;

    public void postUpdate(Long publishedTimestamp) {
        this.order.postUpdate(publishedTimestamp);
    }

    public static OrderDto toDto(OrderVo vo) {
        OrderLocalDto orderDto = OrderLocalDto.toDto(vo.getOrder());
        return new OrderDto(orderDto);
    }

    public static OrderDto toDto(OrderLocalDto order) {
        return new OrderDto(order);
    }
}
