package org.example.order.contract.order.messaging.event;

import org.example.order.contract.order.messaging.type.DlqOrderType;
import org.example.order.contract.order.messaging.type.MessageMethodType;

/**
 * API 라우팅 메시지 계약
 * - Local 메시지를 API 경유/전달할 때 사용
 */
public record OrderApiMessage(
        DlqOrderType category,
        Long id,
        MessageMethodType methodType,
        Long publishedTimestamp
) {
    public static OrderApiMessage fromLocal(OrderLocalMessage local) {
        return new OrderApiMessage(
                DlqOrderType.ORDER_API,
                local.id(),
                local.methodType(),
                local.publishedTimestamp()
        );
    }
}
