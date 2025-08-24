package org.example.order.core.infra.lock.config;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.lock.props.RedissonLockProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 자동 구성
 *
 * 생성 조건:
 *  - lock.enabled = true
 *  - lock.redisson.enabled = true
 *  - 클래스패스에 org.redisson.Redisson 존재
 *  - 기존에 RedissonClient 빈이 없을 때만 생성
 *
 * 주소 우선순위:
 *  1) lock.redisson.address                 (ex: redis://127.0.0.1:6379 또는 127.0.0.1:6379)
 *  2) lock.redisson.uri                     (프로퍼티 클래스엔 없지만 Environment에서 직접 조회)
 *  3) spring.data.redis.host + spring.data.redis.port
 *
 * 어느 것도 없으면 명확한 예외로 실패(원인 추적 용이).
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedissonLockProperties.class)
@ConditionalOnClass(Redisson.class)
@ConditionalOnProperty(name = {"lock.enabled", "lock.redisson.enabled"}, havingValue = "true", matchIfMissing = false)
public class RedissonLockAutoConfig {

    private final RedissonLockProperties props;

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(Environment env) {
        String endpoint = firstNonBlank(
                props.getAddress(),
                env.getProperty("lock.redisson.uri"),
                buildFromHostPort(env.getProperty("spring.data.redis.host"),
                        env.getProperty("spring.data.redis.port", Integer.class))
        );

        if (isBlank(endpoint)) {
            throw new IllegalStateException(
                    "Redisson is enabled but no Redis endpoint provided. " +
                            "Set one of: lock.redisson.address, lock.redisson.uri, " +
                            "or spring.data.redis.host/port."
            );
        }

        String normalized = normalize(endpoint);

        Config config = new Config();
        config.useSingleServer()
                .setAddress(normalized)
                .setDatabase(props.getDatabase())
                .setPassword(blankToNull(props.getPassword()))
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(1);

        return Redisson.create(config);
    }

    // ----------------- helpers -----------------
    private static String buildFromHostPort(String host, Integer port) {
        if (isBlank(host) || port == null) return null;
        return "redis://" + host.trim() + ":" + port;
    }

    private static String normalize(String addr) {
        String v = addr.trim();
        if (!v.startsWith("redis://") && !v.startsWith("rediss://")) {
            return "redis://" + v;
        }
        return v;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (!isBlank(v)) return v;
        return null;
    }

    private static boolean isBlank(String v) { return v == null || v.isBlank(); }
    private static String blankToNull(String v) { return isBlank(v) ? null : v; }
}
