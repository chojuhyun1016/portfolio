package org.example.order.core.infra.lock.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.annotation.DistributedLock;
import org.example.order.core.infra.lock.annotation.DistributedLockT;
import org.example.order.core.infra.lock.service.LockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class NamedLockTestService implements LockService {

    private final AtomicInteger sequence = new AtomicInteger();

    public void clear() {
        sequence.set(0);
    }

    @DistributedLock(key = "#key", type = "namedLock", keyStrategy = "sha256", waitTime = 10000, leaseTime = 15000)
    public String runWithLock(String key) {
        int order = sequence.incrementAndGet();
        String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        log.info("[LOCK] 진입 - key={}, order={}", key, order);
        log.info("[DistributedLock] Transaction ID: {}", txName);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[LOCK] 종료 - order={}", order);
        return txName;
    }

    @DistributedLockT(key = "#key", type = "namedLock", keyStrategy = "sha256", waitTime = 10000, leaseTime = 15000)
    public String runWithLockT(String key) {
        int order = sequence.incrementAndGet();
        String txName = TransactionSynchronizationManager.getCurrentTransactionName();
        log.info("[LOCK T] 진입 - key={}, order={}", key, order);
        log.info("[DistributedLock T] Transaction ID: {}", txName);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[LOCK T] 종료 - order={}", order);
        return txName;
    }
}
