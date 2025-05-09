// 📦 package org.example.order.core.application.order.model;

package org.example.order.core.application.order.dto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.core.application.order.dto.command.OrderSyncCommand;
import org.example.order.core.application.order.mapper.OrderSyncMapper;

/**
 * Local Order 데이터 모델 (Application 계층)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDataModel {

    private OrderSyncCommand order;

    public void updatePublishedTimestamp(Long publishedTimestamp) {
        this.order.updatePublishedTimestamp(publishedTimestamp);
    }

    public static OrderDataModel fromEntityModel(OrderEntityModel vo) {
        OrderSyncCommand orderDto = OrderSyncMapper.toCommand(vo.getOrder());
        return new OrderDataModel(orderDto);
    }

    public static OrderDataModel fromCommand(OrderSyncCommand order) {
        return new OrderDataModel(order);
    }
}
