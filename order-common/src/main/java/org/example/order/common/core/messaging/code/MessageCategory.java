package org.example.order.common.core.messaging.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.type.CodeEnum;

@Getter
@RequiredArgsConstructor
public enum MessageCategory implements CodeEnum {
    ORDER_LOCAL("ORDER_LOCAL"),
    ORDER_API("ORDER_API"),
    ORDER_CRUD("ORDER_CRUD"),

    ORDER_DLQ("ORDER_DLQ"),
    ORDER_ALARM("ORDER_ALARM"),

    RETURN_LOCAL("RETURN_LOCAL"),
    RETURN_API("RETURN_API"),
    RETURN_CRUD("RETURN_CRUD");

    private final String text;
}
