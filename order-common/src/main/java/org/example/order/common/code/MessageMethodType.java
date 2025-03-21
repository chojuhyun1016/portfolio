package org.example.order.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageMethodType implements CodeEnum {
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE");

    private final String text;
}
