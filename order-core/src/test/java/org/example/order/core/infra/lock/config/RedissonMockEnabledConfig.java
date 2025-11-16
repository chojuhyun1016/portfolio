//package org.example.order.core.infra.lock.config;
//
//import org.example.order.core.infra.lock.lock.LockCallback;
//import org.example.order.core.infra.lock.lock.LockExecutor;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.ReentrantLock;
//
//@Configuration
//@ConditionalOnProperty(name = "lock.enabled", havingValue = "true")
//public class RedissonMockEnabledConfig {
//
//    @Bean(name = "redissonLock")
//    @ConditionalOnProperty(name = "lock.redisson.enabled", havingValue = "true")
//    public LockExecutor redissonMockExecutor() {
//        return new InMemoryReentrantLockExecutor();
//    }
//
//    static class InMemoryReentrantLockExecutor implements LockExecutor {
//        private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
//
//        @Override
//        public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
//            Objects.requireNonNull(key, "key");
//            ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
//
//            boolean acquired;
//
//            try {
//                acquired = lock.tryLock(waitTime, TimeUnit.MILLISECONDS);
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                throw ie;
//            }
//
//            if (!acquired) {
//                throw new IllegalStateException("Failed to acquire lock: " + key);
//            }
//
//            try {
//                return callback.call();
//            } finally {
//                lock.unlock();
//            }
//        }
//    }
//}
