package org.example.order.contract.order.messaging.event;

import org.example.order.contract.shared.op.Operation;
import org.example.order.contract.order.messaging.type.MessageOrderType;

/**
 * API 라우팅 메시지 계약
 * - Local 메시지를 API 경유/전달할 때 사용
 */
public record OrderApiMessage(
        Operation operation,
        MessageOrderType orderType,
        Long id,
        Long publishedTimestamp
) {
    public static OrderApiMessage fromLocal(OrderLocalMessage local) {
        return new OrderApiMessage(
                local.operation(),
                MessageOrderType.ORDER_API,
                local.id(),
                local.publishedTimestamp()
        );
    }
}
