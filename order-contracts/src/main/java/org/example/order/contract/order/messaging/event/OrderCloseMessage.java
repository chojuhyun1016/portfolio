package org.example.order.contract.order.messaging.event;

import org.example.order.contract.shared.op.Operation;
import org.example.order.contract.order.messaging.type.MessageOrderType;

/**
 * Remote Close 메시지 계약
 * - 외부/원격 시스템에 주문 종료/닫기 이벤트 전달
 */
public record OrderCloseMessage(
        Operation operation,
        MessageOrderType orderType,
        Long orderId

) {
    public static OrderCloseMessage of(Long orderId, Operation operation) {
        return new OrderCloseMessage(operation, MessageOrderType.ORDER_REMOTE, orderId);
    }
}
