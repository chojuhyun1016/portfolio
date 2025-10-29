package org.example.order.cache.core.support;

import lombok.Getter;

import java.time.Duration;

/**
 * 키/TTL 매니저 (예시용)
 * - 실서비스에서는 도메인별 Enum 분리/버전 관리 권장
 */
public enum RedisKeyManager {

    LOGIN_TOKEN("login:token:%s", Duration.ofHours(6)),
    ORDER_LOCK("order:lock:%s", Duration.ofSeconds(30)),
    USER_CART("cart:user:%s", Duration.ofHours(12)),
    USER_PROFILE("user:profile:%s", Duration.ofDays(1));

    private final String pattern;

    @Getter
    private final Duration ttl;

    RedisKeyManager(String pattern, Duration ttl) {
        this.pattern = pattern;
        this.ttl = ttl;
    }

    public String format(Object... args) {
        return String.format(pattern, args);
    }
}
