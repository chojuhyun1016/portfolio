package org.example.order.domain.order.value;

/**
 * 주문번호 VO (Value Object)
 */
public record OrderNumber(String value) {
    public OrderNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderNumber는 비어 있을 수 없습니다.");
        }
    }

    /**
     * 주문번호 마스킹 (마지막 4자리 제외)
     * @return 마스킹된 문자열
     */
    public String masked() {
        return value.replaceAll(".(?=.{4})", "*");
    }

    @Override
    public String toString() {
        return value;
    }
}
