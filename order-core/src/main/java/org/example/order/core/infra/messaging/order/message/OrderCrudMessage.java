package org.example.order.core.infra.messaging.order.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.infra.messaging.order.code.DlqOrderType;
import org.example.order.common.core.messaging.code.MessageMethodType;

@Getter
@NoArgsConstructor
@ToString
public class OrderCrudMessage extends DlqMessage {
    private MessageMethodType methodType;
    private OrderDto dto;

    public OrderCrudMessage(OrderApiMessage orderApiMessage, OrderDto dto) {
        super(DlqOrderType.ORDER_CRUD);
        this.methodType = orderApiMessage.getMethodType();
        this.dto = dto;
    }

    @Deprecated
    public OrderCrudMessage(MessageMethodType methodType, OrderDto dto) {
        super(DlqOrderType.ORDER_CRUD);
        this.methodType = methodType;
        this.dto = dto;
    }

    public static OrderCrudMessage toMessage(OrderApiMessage orderApiMessage, OrderDto dto) {
        return new OrderCrudMessage(orderApiMessage, dto);
    }

    @Deprecated
    public static OrderCrudMessage test(MessageMethodType methodType, OrderDto dto) {
        return new OrderCrudMessage(methodType, dto);
    }
}
