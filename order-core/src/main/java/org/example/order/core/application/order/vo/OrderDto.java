package org.example.order.core.application.order.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.core.application.order.command.OrderSyncCommand;

/**
 * local order data
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDto {
    private OrderSyncCommand order;

    public void postUpdate(Long publishedTimestamp) {
        this.order.postUpdate(publishedTimestamp);
    }

    public static OrderDto toDto(OrderVo vo) {
        OrderSyncCommand orderDto = OrderSyncCommand.toDto(vo.getOrder());
        return new OrderDto(orderDto);
    }

    public static OrderDto toDto(OrderSyncCommand order) {
        return new OrderDto(order);
    }
}
