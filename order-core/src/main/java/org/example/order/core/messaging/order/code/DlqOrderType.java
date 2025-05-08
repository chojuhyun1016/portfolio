package org.example.order.core.messaging.order.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.messaging.code.DlqType;

/**
 * Order 서비스 전용 DlqType Enum
 */
@Getter
@RequiredArgsConstructor
public enum DlqOrderType implements DlqType {
    ORDER_LOCAL("ORDER_LOCAL"),
    ORDER_API("ORDER_API"),
    ORDER_CRUD("ORDER_CRUD"),
    ORDER_REMOTE("ORDER_REMOTE");

    private final String text;
}
