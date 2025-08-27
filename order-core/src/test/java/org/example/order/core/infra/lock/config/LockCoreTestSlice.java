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

@Configuration
@ConditionalOnProperty(name = "lock.enabled", havingValue = "true")
public class LockCoreTestSlice {

    @Bean(name = "sha256")
    public LockKeyGenerator sha256() {
        return new SHA256LockKeyGenerator();
    }

    @Bean(name = "simple")
    public LockKeyGenerator simple() {
        return new SimpleLockKeyGenerator();
    }

    @Bean(name = "spel")
    public LockKeyGenerator spel() {
        return new SpelLockKeyGenerator();
    }

    @Bean
    public LockKeyGeneratorFactory lockKeyGeneratorFactory(Map<String, LockKeyGenerator> generators) {
        return new LockKeyGeneratorFactory(generators);
    }

    @Bean
    public LockExecutorFactory lockExecutorFactory(Map<String, LockExecutor> executors) {
        return new LockExecutorFactory(executors);
    }

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
