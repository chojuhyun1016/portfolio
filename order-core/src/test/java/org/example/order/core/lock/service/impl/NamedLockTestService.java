package org.example.order.core.lock.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.lock.annotation.DistributedLock;
import org.example.order.core.lock.service.LockService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class NamedLockTestService implements LockService {

    private final AtomicInteger sequence = new AtomicInteger();

    @DistributedLock(key = "#key", type = "namedLock", keyStrategy = "simple")
    public String runWithLock(String key) throws InterruptedException {
        int order = sequence.incrementAndGet();
        log.info("[LOCK] 진입 - key={}, order={}", key, order);
        Thread.sleep(500);
        log.info("[LOCK] 종료 - order={}", order);
        return String.valueOf(order);
    }
}
