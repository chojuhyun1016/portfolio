package org.example.order.core.application.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.application.message.DlqMessage;
import org.example.order.common.code.DlqType;
import org.example.order.common.code.MessageMethodType;
import org.example.order.core.application.dto.OrderDto;

@Getter
@NoArgsConstructor
@ToString
public class OrderCrudMessage extends DlqMessage {
    private MessageMethodType methodType;
    private OrderDto dto;

    public OrderCrudMessage(OrderApiMessage orderApiMessage, OrderDto dto) {
        super(DlqType.ORDER_CRUD);
        this.methodType = orderApiMessage.getMethodType();
        this.dto = dto;
    }

    @Deprecated
    public OrderCrudMessage(MessageMethodType methodType, OrderDto dto) {
        super(DlqType.ORDER_CRUD);
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
