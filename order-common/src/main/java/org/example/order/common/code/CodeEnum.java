package org.example.order.common.code;

public interface CodeEnum {
    String getText();

    default String getCode() {
        return toString();
    }
}
