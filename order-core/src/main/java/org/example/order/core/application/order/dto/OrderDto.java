package org.example.order.core.application.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.core.application.order.command.OrderSyncCommand;
import org.example.order.core.application.order.mapper.OrderSyncMapper;

/**
 * Local Order 데이터 DTO (Application 계층)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDto {

    private OrderSyncCommand order;

    public void updatePublishedTimestamp(Long publishedTimestamp) {
        this.order.updatePublishedTimestamp(publishedTimestamp);
    }

    public static OrderDto fromVo(OrderVo vo) {
        OrderSyncCommand orderDto = OrderSyncMapper.toCommand(vo.getOrder());
        return new OrderDto(orderDto);
    }

    public static OrderDto fromCommand(OrderSyncCommand order) {
        return new OrderDto(order);
    }
}
