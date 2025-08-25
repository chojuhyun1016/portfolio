// src/test/java/org/example/order/core/infra/lock/config/LockCoreTestSlice.java
package org.example.order.core.infra.lock.config;

import org.example.order.core.infra.lock.aspect.DistributedLockAspect;
import org.example.order.core.infra.lock.factory.LockExecutorFactory;
import org.example.order.core.infra.lock.factory.LockKeyGeneratorFactory;
import org.example.order.core.infra.lock.key.LockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SHA256LockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SimpleLockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SpelLockKeyGenerator;
import org.example.order.core.infra.lock.lock.LockExecutor;
import org.example.order.core.infra.lock.lock.LockCallback;
import org.example.order.core.infra.lock.support.TransactionalOperator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 테스트 전용 "락 코어" 슬라이스 설정.
 * - 운영 설정/외부 라이브러리 의존 제거
 * - lock.enabled=true 일 때만 로드
 */
@Configuration
@ConditionalOnProperty(name = "lock.enabled", havingValue = "true")
public class LockCoreTestSlice {

    // === 기본 KeyGenerator 3종 (빈 이름이 type 문자열 키가 됨) ===
    @Bean(name = "sha256")
    public LockKeyGenerator sha256() { return new SHA256LockKeyGenerator(); }

    @Bean(name = "simple")
    public LockKeyGenerator simple() { return new SimpleLockKeyGenerator(); }

    @Bean(name = "spel")
    public LockKeyGenerator spel() { return new SpelLockKeyGenerator(); }

    // === 팩토리들 ===
    @Bean
    public LockKeyGeneratorFactory lockKeyGeneratorFactory(Map<String, LockKeyGenerator> generators) {
        return new LockKeyGeneratorFactory(generators);
    }

    @Bean
    public LockExecutorFactory lockExecutorFactory(Map<String, LockExecutor> executors) {
        return new LockExecutorFactory(executors);
    }

    // === 트랜잭션 연산자: 테스트에선 즉시 실행으로 대체 ===
    @Bean
    public TransactionalOperator transactionalOperator() {
        return new TransactionalOperator() {
            @Override
            public Object runWithExistingTransaction(LockCallback<Object> callback) throws Throwable {
                return callback.call();
            }
            @Override
            public Object runWithNewTransaction(LockCallback<Object> callback) throws Throwable {
                return callback.call();
            }
        };
    }

    @Bean
    public DistributedLockAspect distributedLockAspect(LockKeyGeneratorFactory keyGeneratorFactory,
                                                       LockExecutorFactory lockExecutorFactory,
                                                       TransactionalOperator transactionalOperator) {
        return new DistributedLockAspect(keyGeneratorFactory, lockExecutorFactory, transactionalOperator);
    }
}
