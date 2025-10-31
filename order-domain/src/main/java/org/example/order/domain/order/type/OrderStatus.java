package org.example.order.domain.order.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 주문 상태 Enum
 * - 주문의 라이프사이클 상태를 정의합니다.
 */
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

    /**
     * 코드로 OrderStatus를 반환합니다.
     *
     * @param code 주문 상태 코드
     * @return OrderStatus Enum
     * @throws IllegalArgumentException 잘못된 코드일 경우
     */
    public static OrderStatus fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("OrderStatus code cannot be null");
        }

        return Arrays.stream(values())
                .filter(v -> v.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid OrderStatus code: " + code));
    }
}
