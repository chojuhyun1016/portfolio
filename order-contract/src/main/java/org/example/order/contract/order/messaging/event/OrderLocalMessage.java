package org.example.order.contract.order.messaging.event;

import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.order.messaging.type.MessageMethodType;

/**
 * Local 이벤트 메시지 계약
 * - 내부에서 최초 발행되는 이벤트를 표준화
 */
public record OrderLocalMessage(
        MessageOrderType category,
        Long id,
        MessageMethodType methodType,
        Long publishedTimestamp
) {
    public static OrderLocalMessage of(Long id, MessageMethodType methodType, Long ts) {
        return new OrderLocalMessage(MessageOrderType.ORDER_LOCAL, id, methodType, ts);
    }
}
