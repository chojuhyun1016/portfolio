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
 * - validate() 외에 isValid()/invalidReason() 정적 헬퍼 제공(스트림 필터링/분기에 사용)
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

        if (order.orderId() == null) {
            throw new IllegalArgumentException("orderId is null");
        }
    }

    public static boolean isValid(OrderCrudConsumerDto d) {
        return d != null
                && d.getOperation() != null
                && d.getOrder() != null
                && d.getOrder().orderId() != null;
    }

    public static String invalidReason(OrderCrudConsumerDto d) {
        if (d == null) {
            return "payload is null";
        }

        if (d.getOperation() == null) {
            return "operation is null";
        }

        if (d.getOrder() == null) {
            return "order is null";
        }

        if (d.getOrder().orderId() == null) {
            return "orderId is null";
        }

        return "unknown";
    }

    /**
     * 계약 Payload -> 불변 record(LocalOrderSync)로 변환
     * - LocalOrderSync는 record이므로 canonical constructor로 생성해야 한다.
     * - failure 플래그는 기본 false
     */
    private static LocalOrderSync toLocalOrderDto(OrderPayload p) {
        if (p == null) {
            return null;
        }

        return new LocalOrderSync(
                p.id(),
                p.userId(),
                p.userNumber(),
                p.orderId(),
                p.orderNumber(),
                p.orderPrice(),
                p.deleteYn(),
                p.version(),
                p.createdUserId(),
                p.createdUserType(),
                p.createdDatetime(),
                p.modifiedUserId(),
                p.modifiedUserType(),
                p.modifiedDatetime(),
                p.publishedTimestamp(),
                false
        );
    }
}
