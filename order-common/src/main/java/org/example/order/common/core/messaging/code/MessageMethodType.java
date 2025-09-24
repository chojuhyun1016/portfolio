package org.example.order.common.core.messaging.code;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.type.CodeEnum;
import org.example.order.common.support.json.MessageMethodTypeDeserializer;

@Getter
@RequiredArgsConstructor
@JsonDeserialize(using = MessageMethodTypeDeserializer.class)
public enum MessageMethodType implements CodeEnum {
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE");

    private final String text;
}
