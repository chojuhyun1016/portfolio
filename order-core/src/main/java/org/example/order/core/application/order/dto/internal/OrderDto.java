// 📦 package org.example.order.core.application.order.model;

package org.example.order.core.application.order.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.core.application.order.mapper.OrderMapper;

/**
 * Local Order 데이터 모델 (Application 계층)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDto {

    private LocalOrderDto order;

    public void updatePublishedTimestamp(Long publishedTimestamp) {
        this.order.updatePublishedTimestamp(publishedTimestamp);
    }

    public static OrderDto fromEntityModel(OrderEntityDto vo) {
        LocalOrderDto orderDto = OrderMapper.toDto(vo.getOrder());
        return new OrderDto(orderDto);
    }

    public static OrderDto fromCommand(LocalOrderDto order) {
        return new OrderDto(order);
    }
}
