package org.example.order.common.core.code;

public interface CodeEnum {
    String getText();

    default String getCode() {
        return toString();
    }
}
