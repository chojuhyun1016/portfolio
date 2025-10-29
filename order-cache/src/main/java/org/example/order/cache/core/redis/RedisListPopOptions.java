package org.example.order.cache.core.redis;

/**
 * Redis List pop 옵션
 * - loop: 최대 반복 횟수 (null 이면 구현 기본값)
 * - blockingTimeoutSeconds: 블로킹 대기시간(초); 0 또는 null이면 논블로킹
 */
public final class RedisListPopOptions {
    private final Integer loop;
    private final Long blockingTimeoutSeconds;

    private RedisListPopOptions(Builder b) {
        this.loop = b.loop;
        this.blockingTimeoutSeconds = b.blockingTimeoutSeconds;
    }

    public Integer getLoop() {
        return loop;
    }

    public Long getBlockingTimeoutSeconds() {
        return blockingTimeoutSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Integer loop;
        private Long blockingTimeoutSeconds;

        public Builder loop(Integer loop) {
            this.loop = loop;

            return this;
        }

        public Builder blockingTimeoutSeconds(Long seconds) {
            this.blockingTimeoutSeconds = seconds;

            return this;
        }

        public RedisListPopOptions build() {
            return new RedisListPopOptions(this);
        }
    }
}
