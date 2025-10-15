package org.example.order.worker.dto.consumer;

import lombok.Getter;
import lombok.ToString;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.payload.OrderPayload;
import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.internal.OrderSyncDto;

/**
 * OrderCrudConsumerDto
 * 목적
 * - CRUD 토픽에서 수신한 계약 이벤트(OrderCrudMessage)를
 * worker 내부에서 사용하는 전용 DTO로 변환해 운반한다.
 */
@Getter
@ToString
public class OrderCrudConsumerDto {

    private final Operation operation;
    private final OrderSyncDto order;

    public OrderCrudConsumerDto(Operation operation, OrderSyncDto order) {
        this.operation = operation;
        this.order = order;
    }

    public static OrderCrudConsumerDto from(OrderCrudMessage msg) {
        if (msg == null) {
            return null;
        }

        OrderSyncDto d = toLocalOrderDto(msg.payload());

        return new OrderCrudConsumerDto(msg.operation(), d);
    }

    public void validate() {
        if (operation == null) {
            throw new IllegalArgumentException("operation is null");
        }

        if (order == null) {
            throw new IllegalArgumentException("order is null");
        }

        if (order.getOrderId() == null) {
            throw new IllegalArgumentException("orderId is null");
        }
    }

    private static OrderSyncDto toLocalOrderDto(OrderPayload p) {
        OrderSyncDto d = new OrderSyncDto();

        if (p == null) {
            return d;
        }

        d.updatePublishedTimestamp(null);
        d.updateUserId(p.userId());
        d.updateOrderId(p.orderId());
        d.updateOrderNumber(p.orderNumber());
        d.updateUserNumber(p.userNumber());
        d.updateOrderPrice(p.orderPrice());

        return d;
    }
}
