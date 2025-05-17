package org.example.order.core.messaging.order.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.application.order.dto.model.OrderDataModelDto;
import org.example.order.core.messaging.order.code.DlqOrderType;
import org.example.order.common.core.messaging.code.MessageMethodType;

@Getter
@NoArgsConstructor
@ToString
public class OrderCrudEvent extends DlqMessage {
    private MessageMethodType methodType;
    private OrderDataModelDto dto;

    public OrderCrudEvent(OrderApiEvent orderApiEvent, OrderDataModelDto dto) {
        super(DlqOrderType.ORDER_CRUD);
        this.methodType = orderApiEvent.getMethodType();
        this.dto = dto;
    }

    @Deprecated
    public OrderCrudEvent(MessageMethodType methodType, OrderDataModelDto dto) {
        super(DlqOrderType.ORDER_CRUD);
        this.methodType = methodType;
        this.dto = dto;
    }

    public static OrderCrudEvent toMessage(OrderApiEvent orderApiEvent, OrderDataModelDto dto) {
        return new OrderCrudEvent(orderApiEvent, dto);
    }

    @Deprecated
    public static OrderCrudEvent test(MessageMethodType methodType, OrderDataModelDto dto) {
        return new OrderCrudEvent(methodType, dto);
    }
}
