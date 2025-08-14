package org.example.order.core.infra.lock.config;

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
import org.example.order.core.infra.lock.support.TransactionalOperator;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({NamedLockProperties.class, RedissonLockProperties.class})
@ConditionalOnProperty(name = "lock.enabled", havingValue = "true")
public class LockManualConfig {

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

    /* ---------- Executors ---------- */
    @Bean(name = "namedLock")
    @ConditionalOnProperty(name = {"lock.named.enabled"}, havingValue = "true")
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(name = "namedLock")
    public LockExecutor namedLockExecutor(NamedLockProperties props, DataSource dataSource) {
        return new NamedLockExecutor(props, dataSource);
    }

    @Bean(name = "redissonLock")
    @ConditionalOnProperty(name = {"lock.redisson.enabled"}, havingValue = "true")
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(name = "redissonLock")
    public LockExecutor redissonLockExecutor(RedissonLockProperties props, RedissonClient client) {
        return new RedissonLockExecutor(props, client);
    }

    /* ---------- Factories ---------- */
    @Bean
    @ConditionalOnMissingBean
    public LockKeyGeneratorFactory lockKeyGeneratorFactory(Map<String, LockKeyGenerator> generators) {
        // 이름 기반 조회(sha256/simple/spell)를 위해 그대로 전달
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
}
