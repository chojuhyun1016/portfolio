package org.example.order.common.core.messaging.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.type.CodeEnum;

@Getter
@RequiredArgsConstructor
public enum MessageMethodType implements CodeEnum {
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE");

    private final String text;
}
