package org.example.order.domain.order.vo;

public record OrderNumber(String value) {
    public OrderNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OrderNumber는 비어 있을 수 없습니다.");
        }
    }

    public String masked() {
        return value.replaceAll(".(?=.{4})", "*");
    }

    @Override
    public String toString() {
        return value;
    }
}
