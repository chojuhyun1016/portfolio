package org.example.order.cache.autoconfig.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CacheToggleProperties
 * ------------------------------------------------------------------------
 * 목적
 * - 캐시 기능 전역/구현별 활성화 토글을 명시적 네임스페이스로 제공한다.
 * <p>
 * 사용 예:
 * order.cache.enabled=true
 * order.cache.redis.enabled=true
 * <p>
 * 구현별 세부 설정은 order.cache.<impl>.* 네임스페이스를 권장한다.
 */
@ConfigurationProperties(prefix = "order.cache")
public class CacheToggleProperties {

    /**
     * 캐시 기능 전역 토글 (기본 false: 명시적으로 켜는 것을 권장)
     */
    private boolean enabled = false;

    /**
     * Redis 구현 토글 (order.cache.enabled 와 AND 조건으로 사용)
     */
    private final RedisToggle redis = new RedisToggle();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RedisToggle getRedis() {
        return redis;
    }

    public static class RedisToggle {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
