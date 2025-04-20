package org.example.order.core.application.order.event.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.event.DlqMessage;
import org.example.order.common.code.DlqType;
import org.example.order.common.code.MessageMethodType;
import org.example.order.core.application.order.vo.OrderDto;

@Getter
@NoArgsConstructor
@ToString
public class OrderCrudEvent extends DlqMessage {
    private MessageMethodType methodType;
    private OrderDto dto;

    public OrderCrudEvent(OrderApiEvent orderApiEvent, OrderDto dto) {
        super(DlqType.ORDER_CRUD);
        this.methodType = orderApiEvent.getMethodType();
        this.dto = dto;
    }

    @Deprecated
    public OrderCrudEvent(MessageMethodType methodType, OrderDto dto) {
        super(DlqType.ORDER_CRUD);
        this.methodType = methodType;
        this.dto = dto;
    }

    public static OrderCrudEvent toMessage(OrderApiEvent orderApiEvent, OrderDto dto) {
        return new OrderCrudEvent(orderApiEvent, dto);
    }

    @Deprecated
    public static OrderCrudEvent test(MessageMethodType methodType, OrderDto dto) {
        return new OrderCrudEvent(methodType, dto);
    }
}
