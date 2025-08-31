package org.example.order.core.infra.messaging.order.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.type.CodeEnum;

@Getter
@RequiredArgsConstructor
public enum MessageCategory implements CodeEnum {
    ORDER_LOCAL("ORDER_LOCAL"),
    ORDER_API("ORDER_API"),
    ORDER_CRUD("ORDER_CRUD"),
    ORDER_REMOTE("ORDER_REMOTE"),
    ORDER_DLQ("ORDER_DLQ"),
    ORDER_ALARM("ORDER_ALARM");

    private final String text;
}
