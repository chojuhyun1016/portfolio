package org.example.order.core.infra.lock.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.aspect.DistributedLockAspect;
import org.example.order.core.infra.lock.factory.LockExecutorFactory;
import org.example.order.core.infra.lock.factory.LockKeyGeneratorFactory;
import org.example.order.core.infra.lock.key.LockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SHA256LockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SimpleLockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SpelLockKeyGenerator;
import org.example.order.core.infra.lock.lock.LockExecutor;
import org.example.order.core.infra.lock.lock.impl.NamedLockExecutor;
import org.example.order.core.infra.lock.lock.impl.RedissonLockExecutor;
import org.example.order.core.infra.lock.props.NamedLockProperties;
import org.example.order.core.infra.lock.props.RedissonLockProperties;
import org.example.order.core.infra.lock.support.TransactionalOperator;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 락 인프라 구성(설정 기반 + @Import 조립)
 * <p>
 * 전역 스위치 제거 → 기능별 스위치로만 제어:
 * - NamedLock   : lock.named.enabled=true  && DataSource 빈 존재
 * - RedissonLock: lock.redisson.enabled=true && RedissonClient 빈 존재
 * <p>
 * 변경점(분리):
 * - RedissonClient는 오직 lock.redisson.*(host/port/password/database) 만 사용합니다.
 * (spring.redis.* 등 다른 키는 읽지 않음)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({NamedLockProperties.class, RedissonLockProperties.class})
public class LockInfraConfig {

    /* ---------- Key Generators ---------- */
    @Bean(name = "sha256")
    @ConditionalOnMissingBean(name = "sha256")
    public LockKeyGenerator sha256KeyGenerator() {
        return new SHA256LockKeyGenerator();
    }

    @Bean(name = "simple")
    @ConditionalOnMissingBean(name = "simple")
    public LockKeyGenerator simpleKeyGenerator() {
        return new SimpleLockKeyGenerator();
    }

    @Bean(name = "spell")
    @ConditionalOnMissingBean(name = "spell")
    public LockKeyGenerator spelKeyGenerator() {
        return new SpelLockKeyGenerator();
    }

    /* ---------- Factories ---------- */
    @Bean
    @ConditionalOnMissingBean
    public LockKeyGeneratorFactory lockKeyGeneratorFactory(Map<String, LockKeyGenerator> generators) {
        return new LockKeyGeneratorFactory(generators);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockExecutorFactory lockExecutorFactory(Map<String, LockExecutor> executors) {
        return new LockExecutorFactory(executors);
    }

    /* ---------- TX Operator & Aspect ---------- */
    @Bean
    @ConditionalOnMissingBean
    public TransactionalOperator transactionalOperator() {
        return new TransactionalOperator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DistributedLockAspect distributedLockAspect(
            LockKeyGeneratorFactory keyFactory,
            LockExecutorFactory executorFactory,
            TransactionalOperator txOperator
    ) {
        return new DistributedLockAspect(keyFactory, executorFactory, txOperator);
    }

    /* ---------- Executors ---------- */
    @Bean(name = "namedLock")
    @ConditionalOnProperty(name = "lock.named.enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(name = "namedLock")
    public LockExecutor namedLockExecutor(NamedLockProperties props, DataSource dataSource) {
        return new NamedLockExecutor(props, dataSource);
    }

    @Bean(name = "redissonLock")
    @ConditionalOnProperty(name = "lock.redisson.enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(name = "redissonLock")
    public LockExecutor redissonLockExecutor(RedissonLockProperties props, RedissonClient client) {
        return new RedissonLockExecutor(props, client);
    }

    /* ---------- Redisson Client ---------- */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnClass(Redisson.class)
    @ConditionalOnMissingBean(RedissonClient.class)
    @ConditionalOnProperty(name = "lock.redisson.enabled", havingValue = "true", matchIfMissing = false)
    public RedissonClient redissonClient(RedissonLockProperties props) {
        String endpoint = buildFromHostPort(props.getHost(), props.getPort());

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(
                    "Redisson is enabled but no Redis endpoint provided. " +
                            "Please set both 'lock.redisson.host' and 'lock.redisson.port'."
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

    // ---------------- helpers ----------------
    private static String buildFromHostPort(String host, Integer port) {
        if (host == null || host.isBlank() || port == null) return null;
        return "redis://" + host.trim() + ":" + port;
    }

    private static String normalize(String addr) {
        String v = addr.trim();
        if (!v.startsWith("redis://") && !v.startsWith("rediss://")) {
            return "redis://" + v;
        }
        return v;
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
