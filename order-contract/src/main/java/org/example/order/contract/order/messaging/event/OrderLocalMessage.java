package org.example.order.contract.order.messaging.event;

import org.example.order.contract.shared.op.Operation;
import org.example.order.contract.order.messaging.type.MessageOrderType;

/**
 * Local 이벤트 메시지 계약
 * - 정상/본선 토픽에 실리는 메시지
 * - DLQ에 보낼 때는 이걸 DeadLetter<OrderLocalMessage>의 payload로 감싼다.
 */
public record OrderLocalMessage(
        Operation operation,
        MessageOrderType orderType,
        Long id,
        Long publishedTimestamp
) {
    public static OrderLocalMessage of(Long id, Operation operation, Long ts) {
        return new OrderLocalMessage(operation, MessageOrderType.ORDER_LOCAL, id, ts);
    }
}
