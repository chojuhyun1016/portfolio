package org.example.order.core.infra.lock.config;

import org.example.order.core.infra.lock.lock.LockCallback;
import org.example.order.core.infra.lock.lock.LockExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Configuration
public class RedissonMockUnitConfig {

    @Bean(name = "redissonLock")
    @ConditionalOnMissingBean(name = "redissonLock")
    public LockExecutor redissonLock() {
        return new InMemoryRedissonLockExecutor();
    }

    static class InMemoryRedissonLockExecutor implements LockExecutor {
        private static final Map<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

        private ReentrantLock lockFor(String key) {
            return LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
        }

        @Override
        public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
            ReentrantLock lock = lockFor(key);
            boolean acquired = false;
            try {
                long waitMs = Math.max(0, waitTime);
                acquired = lock.tryLock(waitMs, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    throw new IllegalStateException("Failed to acquire lock for key=" + key);
                }
                return callback.call();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while acquiring lock for key=" + key, e);
            } finally {
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}
