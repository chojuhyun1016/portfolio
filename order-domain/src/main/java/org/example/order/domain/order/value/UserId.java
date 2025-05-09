package org.example.order.domain.order.value;

/**
 * 사용자 ID VO (Value Object)
 */
public record UserId(Long value) {
    public UserId {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("UserId는 0 이상이어야 합니다.");
        }
    }

    /**
     * 시스템 사용자 여부 (0 ID)
     */
    public boolean isSystemUser() {
        return value == 0L;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
