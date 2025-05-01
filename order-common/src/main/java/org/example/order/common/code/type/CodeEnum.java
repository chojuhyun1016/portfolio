package org.example.order.common.code.type;

public interface CodeEnum {
    String getText();

    default String getCode() {
        return toString();
    }
}
