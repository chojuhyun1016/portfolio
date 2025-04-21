package org.example.order.core.infra.redis.support;

import lombok.Getter;

import java.time.Duration;

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
