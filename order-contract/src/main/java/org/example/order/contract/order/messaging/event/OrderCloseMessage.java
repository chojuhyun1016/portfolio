package org.example.order.contract.order.messaging.event;

import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.order.messaging.type.MessageMethodType;

/**
 * Remote Close 메시지 계약
 * - 외부/원격 시스템에 주문 종료/닫기 이벤트 전달
 */
public record OrderCloseMessage(
        MessageOrderType category,
        Long orderId,
        MessageMethodType methodType
) {
    public static OrderCloseMessage of(Long orderId, MessageMethodType methodType) {
        return new OrderCloseMessage(MessageOrderType.ORDER_REMOTE, orderId, methodType);
    }
}
