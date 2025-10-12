package org.example.order.contract.order.messaging.event;

import org.example.order.contract.order.messaging.payload.OrderPayload;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.order.messaging.type.MessageMethodType;

/**
 * CRUD 메시지 계약
 * - CRUD 처리 서비스(consumer)로 넘길 때 표준 형태
 */
public record OrderCrudMessage(
        MessageOrderType category,
        MessageMethodType methodType,
        OrderPayload payload
) {
    public static OrderCrudMessage of(MessageMethodType methodType, OrderPayload payload) {
        return new OrderCrudMessage(MessageOrderType.ORDER_CRUD, methodType, payload);
    }
}
