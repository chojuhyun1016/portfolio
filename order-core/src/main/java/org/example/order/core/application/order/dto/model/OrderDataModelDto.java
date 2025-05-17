// 📦 package org.example.order.core.application.order.model;

package org.example.order.core.application.order.dto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.core.application.order.dto.command.OrderSyncCommandDto;
import org.example.order.core.application.order.mapper.OrderSyncCommandMapper;

/**
 * Local Order 데이터 모델 (Application 계층)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDataModelDto {

    private OrderSyncCommandDto order;

    public void updatePublishedTimestamp(Long publishedTimestamp) {
        this.order.updatePublishedTimestamp(publishedTimestamp);
    }

    public static OrderDataModelDto fromEntityModel(OrderEntityModelDto vo) {
        OrderSyncCommandDto orderDto = OrderSyncCommandMapper.toCommand(vo.getOrder());
        return new OrderDataModelDto(orderDto);
    }

    public static OrderDataModelDto fromCommand(OrderSyncCommandDto order) {
        return new OrderDataModelDto(order);
    }
}
