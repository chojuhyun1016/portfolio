package org.example.order.core.domain.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    CREATED("C", "주문 생성"),
    PAID("P", "결제 완료"),
    SHIPPED("S", "배송 중"),
    COMPLETED("D", "배송 완료"),
    CANCELLED("X", "주문 취소");

    private final String code;
    private final String description;

    public static OrderStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid code: " + code));
    }
}
