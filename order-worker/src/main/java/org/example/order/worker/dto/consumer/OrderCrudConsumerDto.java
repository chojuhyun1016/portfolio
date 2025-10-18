package org.example.order.worker.dto.consumer;

import lombok.Getter;
import lombok.ToString;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.payload.OrderPayload;
import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;

/**
 * OrderCrudConsumerDto
 * - CRUD 토픽 계약 이벤트(OrderCrudMessage)를 worker 내부용 DTO로 변환
 */
@Getter
@ToString
public class OrderCrudConsumerDto {

    private final Operation operation;
    private final LocalOrderSync order;

    public OrderCrudConsumerDto(Operation operation, LocalOrderSync order) {
        this.operation = operation;
        this.order = order;
    }

    public static OrderCrudConsumerDto from(OrderCrudMessage msg) {
        if (msg == null) {
            return null;
        }

        OrderPayload p = msg.payload();
        LocalOrderSync d = toLocalOrderDto(p);

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

    private static LocalOrderSync toLocalOrderDto(OrderPayload p) {
        LocalOrderSync d = new LocalOrderSync();

        if (p == null) {
            return d;
        }

        // 식별/주문 기본
        d.setId(p.id());
        d.setOrderId(p.orderId());
        d.setOrderNumber(p.orderNumber());

        // 사용자/가격
        d.setUserId(p.userId());
        d.setUserNumber(p.userNumber());
        d.setOrderPrice(p.orderPrice());

        // 삭제/버전
        d.setDeleteYn(p.deleteYn());
        d.setVersion(p.version());

        // 생성/수정 메타
        d.updateCreatedMeta(p.createdUserId(), p.createdUserType(), p.createdDatetime());
        d.updateModifiedMeta(p.modifiedUserId(), p.modifiedUserType(), p.modifiedDatetime());

        // 발행 시각(epoch millis)
        d.setPublishedTimestamp(p.publishedTimestamp());

        return d;
    }
}
