package org.example.order.core.application.order.cache.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OrderCacheProperties
 * ------------------------------------------------------------------------
 * 목적
 * - 캐시 관련 코어 레벨 속성 바인딩.
 * - 기본 TTL 등 실행 옵션을 외부화한다.
 * <p>
 * 설정 예시:
 * order.cache.enabled=true
 * order.cache.redis.enabled=true
 * order.application.cache.default-ttl-seconds=300
 */
@ConfigurationProperties(prefix = "order.application.cache")
public class OrderCacheProperties {

    /**
     * 캐시 기본 TTL(초)
     * - null 이면 기본 TTL을 사용하지 않고, 호출자가 직접 TTL을 지정해야 한다.
     */
    private Long defaultTtlSeconds;

    public Long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(Long defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }
}
