package org.example.order.common.core.code.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.CodeEnum;

@Getter
@RequiredArgsConstructor
public enum DlqType implements CodeEnum {
    ORDER_LOCAL("ORDER_LOCAL"),
    ORDER_API("ORDER_API"),
    ORDER_CRUD("ORDER_CRUD"),
    ORDER_REMOTE("ORDER_REMOTE");

    private final String text;
}
