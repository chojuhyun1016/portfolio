package org.example.order.core.infra.lock.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.annotation.DistributedLock;
import org.example.order.core.infra.lock.annotation.DistributedLockT;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 통합테스트 전용 서비스 (프로덕션 코드 아님)
 * - 동시성 계측을 위한 카운터/동시진입 관찰
 * - @DistributedLock / @DistributedLockT AOP 경로를 실제 타게 함
 */
@Slf4j
@Service
public class LockedServices {

    private final AtomicInteger totalCalls = new AtomicInteger(0);
    private final AtomicInteger concurrent = new AtomicInteger(0);
    @Getter
    private final AtomicInteger maxObserved = new AtomicInteger(0);

    public void reset() {
        totalCalls.set(0);
        concurrent.set(0);
        maxObserved.set(0);
    }

    public AtomicInteger getTotalCalls() {
        return totalCalls;
    }

    // ===== NamedLock (SpEL 키) - 기존 트랜잭션(REQUIRED) =====
    @DistributedLock(
            key = "'named:order:' + #orderId",
            type = "namedLock",
            keyStrategy = "spell",
            waitTime = 60000,
            leaseTime = 5000
    )
    public int namedSpel(Long orderId, long sleepMillis) {
        return doCritical("namedSpel", sleepMillis);
    }

    // ===== NamedLock (SHA256 키) - 새로운 트랜잭션(REQUIRES_NEW) =====
    @DistributedLockT(
            key = "named:sha256",
            type = "namedLock",
            keyStrategy = "sha256",
            waitTime = 60000,
            leaseTime = 5000
    )
    public int namedSha256_TxNew(String payload, long sleepMillis) {
        return doCritical("namedSha256_TxNew", sleepMillis);
    }

    // ===== Redisson (SpEL 키) - REQUIRED =====
    @DistributedLock(
            key = "'redisson:spel:' + #orderId",
            type = "redissonLock",
            keyStrategy = "spell",
            waitTime = 10000,
            leaseTime = 5000
    )
    public int redissonSpel(Long orderId, long sleepMillis) {
        return doCritical("redissonSpel", sleepMillis);
    }

    // ===== Redisson (Simple 키) - REQUIRES_NEW) =====
    @DistributedLockT(
            key = "redisson:simple",
            type = "redissonLock",
            keyStrategy = "simple",
            waitTime = 10000,
            leaseTime = 5000
    )
    public int redissonSimple_TxNew(String payload, long sleepMillis) {
        return doCritical("redissonSimple_TxNew", sleepMillis);
    }

    // === [추가] 단일 파라미터 오버로드 ===
    @DistributedLockT(
            key = "redisson:simple",
            type = "redissonLock",
            keyStrategy = "simple",
            waitTime = 10000,
            leaseTime = 5000
    )
    public int redissonSimple_TxNew(long sleepMillis) {
        return redissonSimple_TxNew("it", sleepMillis);
    }

    private int doCritical(String tag, long sleepMillis) {
        int now = concurrent.incrementAndGet();
        maxObserved.accumulateAndGet(now, Math::max);
        int callNo = totalCalls.incrementAndGet();

        log.info("[{}] enter: call#{} concurrent={}", tag, callNo, now);

        try {
            Thread.sleep(sleepMillis);
            return callNo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            int after = concurrent.decrementAndGet();
            log.info("[{}] exit: call#{} concurrent->{}", tag, callNo, after);
        }
    }
}
