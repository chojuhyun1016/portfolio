package org.example.order.contract.order.messaging.event;

import org.example.order.contract.shared.op.Operation;
import org.example.order.contract.order.messaging.payload.OrderPayload;
import org.example.order.contract.order.messaging.type.MessageOrderType;

/**
 * CRUD 메시지 계약
 * - CRUD 처리 서비스(consumer)로 넘길 때 표준 형태
 */
public record OrderCrudMessage(
        Operation operation,
        MessageOrderType orderType,
        OrderPayload payload
) {
    public static OrderCrudMessage of(Operation methodType, OrderPayload payload) {
        return new OrderCrudMessage(methodType, MessageOrderType.ORDER_CRUD, payload);
    }
}
