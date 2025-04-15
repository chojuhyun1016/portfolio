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
        long wait = waitTime > 0 ? waitTime : redissonProperties.getWaitTime();
        long lease = leaseTime > 0 ? leaseTime : redissonProperties.getLeaseTime();
        long retryInterval = redissonProperties.getRetryInterval();
        int maxRetries = (int) (wait / retryInterval);

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
                        releaseLock(lock, key);
                    }
                }

                log.debug("Redisson lock attempt failed. key={}, attempt={}, retrying...", key, attempt);
                TimeUnit.MILLISECONDS.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("Redisson lock interrupted. key: " + key, e);
            } catch (Exception e) {
                log.error("""
                    Redisson error during lock
                    ├─ key     : {}
                    ├─ attempt : {}
                    └─ error   : {}
                    """, key, attempt, e.getMessage(), e);
                throw new LockAcquisitionException("Redisson lock execution failed for key: " + key, e);
            }
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        throw new LockAcquisitionException("Redisson lock failed for key=" + key + " after " + totalElapsed + "ms");
    }

    private void releaseLock(RLock lock, String key) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released redisson lock. key={}", key);
            } else {
                log.warn("Redisson lock not held by current thread. key={}", key);
            }
        } catch (Exception e) {
            log.error("Failed to release redisson lock. key={}, error={}", key, e.getMessage(), e);
        }
    }
}
