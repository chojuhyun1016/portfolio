package org.example.order.common.code.enums;

public interface CodeEnum {
    String getText();

    default String getCode() {
        return toString();
    }
}
