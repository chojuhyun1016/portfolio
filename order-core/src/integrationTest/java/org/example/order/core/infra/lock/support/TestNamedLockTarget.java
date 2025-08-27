package org.example.order.core.infra.lock.support;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.annotation.DistributedLock;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class TestNamedLockTarget {

    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicInteger concurrent = new AtomicInteger(0);
    private final AtomicInteger maxObserved = new AtomicInteger(0);

    @DistributedLock(key = "'it-named-lock'", type = "namedLock", waitTime = 1500, leaseTime = 2_000)
    public void criticalSection() {
        int now = concurrent.incrementAndGet();
        maxObserved.accumulateAndGet(now, Math::max);

        int value = counter.incrementAndGet();
        log.info("[IT named criticalSection] Thread={} | Value={} | concurrent={}",
                Thread.currentThread().getName(), value, now);
        try {
            Thread.sleep(80);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            concurrent.decrementAndGet();
        }
    }

    public void reset() {
        counter.set(0);
        concurrent.set(0);
        maxObserved.set(0);
    }

    public int getCounter() {
        return counter.get();
    }

    public int getMaxObserved() {
        return maxObserved.get();
    }
}
