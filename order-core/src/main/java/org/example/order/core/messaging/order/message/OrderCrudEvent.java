package org.example.order.core.messaging.order.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.application.order.dto.model.OrderDataModel;
import org.example.order.core.messaging.order.code.DlqOrderType;
import org.example.order.core.messaging.order.code.MessageMethodType;

@Getter
@NoArgsConstructor
@ToString
public class OrderCrudEvent extends DlqMessage {
    private MessageMethodType methodType;
    private OrderDataModel dto;

    public OrderCrudEvent(OrderApiEvent orderApiEvent, OrderDataModel dto) {
        super(DlqOrderType.ORDER_CRUD);
        this.methodType = orderApiEvent.getMethodType();
        this.dto = dto;
    }

    @Deprecated
    public OrderCrudEvent(MessageMethodType methodType, OrderDataModel dto) {
        super(DlqOrderType.ORDER_CRUD);
        this.methodType = methodType;
        this.dto = dto;
    }

    public static OrderCrudEvent toMessage(OrderApiEvent orderApiEvent, OrderDataModel dto) {
        return new OrderCrudEvent(orderApiEvent, dto);
    }

    @Deprecated
    public static OrderCrudEvent test(MessageMethodType methodType, OrderDataModel dto) {
        return new OrderCrudEvent(methodType, dto);
    }
}
