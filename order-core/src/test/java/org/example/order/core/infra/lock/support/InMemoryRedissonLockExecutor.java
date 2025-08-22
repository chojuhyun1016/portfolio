package org.example.order.core.infra.lock.support;

import org.example.order.core.infra.lock.lock.LockCallback;
import org.example.order.core.infra.lock.lock.LockExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * test-unit 전용 "redissonLock" 대체 실행기.
 * - 실제 Redis/Redisson 없이 ReentrantLock 으로 직렬화 보장
 * - LockExecutor/LockCallback 의 '원' 시그니처에 정확히 맞춤 (callback.call())
 * - 빈 이름 "redissonLock" 으로 등록되어 Factory 의 getExecutor("redissonLock") 과 매칭
 */
public class InMemoryRedissonLockExecutor implements LockExecutor {

    private static final Map<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private ReentrantLock lockFor(String key) {
        return LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
    }

    @Override
    public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
        ReentrantLock lock = lockFor(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(Math.max(0, waitTime), TimeUnit.MILLISECONDS);
            if (!acquired) {
                // 테스트에서 원인 파악이 쉽도록 즉시 실패 처리
                throw new IllegalStateException("Failed to acquire lock for key=" + key);
            }
            // ✅ 원본 콜백 시그니처에 맞춰 call() 호출, 반환값을 그대로 전달
            return callback.call();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
