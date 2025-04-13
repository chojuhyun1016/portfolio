package org.example.order.core.lock.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.lock.config.RedissonProperties;
import org.example.order.core.lock.exception.LockAcquisitionException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component("redissonLock")
@RequiredArgsConstructor
public class RedissonLockExecutor implements LockExecutor {

    private final RedissonProperties redissonProperties;
    private final RedissonClient redissonClient;

    @Override
    public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
        long wait = waitTime > 0 ? waitTime : redissonProperties.getWaitTime();         // 전체 최대 대기 시간
        long lease = leaseTime > 0 ? leaseTime : redissonProperties.getLeaseTime();     // 락 유지 시간
        long retryInterval = redissonProperties.getRetryInterval();                     // 재시도 간격
        int maxRetries = (int) (wait / retryInterval);                                  // 최대 시도 횟수

        RLock lock = redissonClient.getLock(key);
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                boolean locked = lock.tryLock(retryInterval, lease, TimeUnit.MILLISECONDS);
                long elapsed = System.currentTimeMillis() - startTime;

                if (locked) {
                    log.debug("Acquired redisson lock. key={}, attempt={}, waited={}ms", key, attempt, elapsed);
                    try {
                        return callback.call();
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                            log.debug("Released redisson lock. key={}", key);
                        } else {
                            log.warn("Lock not held by current thread, cannot release. key={}", key);
                        }
                    }
                } else {
                    log.debug("Redisson lock attempt failed. key={}, attempt={}, retrying...", key, attempt);
                }

                TimeUnit.MILLISECONDS.sleep(retryInterval);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("Thread interrupted during redisson lock wait. key: " + key, e);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("""
                    Redisson error during lock operation
                    ├─ key        : {}
                    ├─ attempt    : {}
                    ├─ waited     : {}ms
                    └─ RootCause  : {}
                    """, key, attempt, elapsed, e.getMessage(), e);
                throw new LockAcquisitionException("Redisson lock execution failed for key: " + key, e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        throw new LockAcquisitionException("Failed to acquire redisson lock for key: " + key + " after " + elapsed + "ms and " + maxRetries + " attempts.");
    }
}
