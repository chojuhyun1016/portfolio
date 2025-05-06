package org.example.order.domain.order.vo;

public record UserId(Long value) {
    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId는 0보다 큰 값이어야 합니다.");
        }
    }

    public boolean isSystemUser() {
        return value == 0L;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
