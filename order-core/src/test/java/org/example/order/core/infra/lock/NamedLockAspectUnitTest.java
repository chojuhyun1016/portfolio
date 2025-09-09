package org.example.order.core.infra.lock;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.annotation.DistributedLock;
import org.example.order.core.infra.lock.config.LockCoreTestSlice;
import org.example.order.core.infra.lock.config.NamedLockMockEnabledConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = NamedLockAspectUnitTest.Boot.class)
@ActiveProfiles("test-unit")
@TestPropertySource(properties = {
        "lock.enabled=true",
        "lock.redisson.enabled=false",
        "lock.named.enabled=true"
})
@Import({LockCoreTestSlice.class, NamedLockMockEnabledConfig.class})
class NamedLockAspectUnitTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Boot {
        @Bean
        DummyTarget target() {
            return new DummyTarget();
        }
    }

    static class DummyTarget {
        private int counter;
        private int concurrent;

        @Getter
        private int maxObserved;

        @DistributedLock(
                type = "namedLock",
                key = "'named:order:' + #p0",
                keyStrategy = "spel",
                waitTime = 2000,
                leaseTime = 2000
        )
        public void critical(long id) {
            int now = incConcurrent();
            maxObserved = Math.max(maxObserved, now);

            try {
                counter++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                decConcurrent();
            }
        }

        private synchronized int incConcurrent() {
            return ++concurrent;
        }

        private synchronized void decConcurrent() {
            --concurrent;
        }

        public int counter() {
            return counter;
        }
    }

    private final DummyTarget target;

    @Autowired
    NamedLockAspectUnitTest(DummyTarget target) {
        this.target = target;
    }

    @BeforeEach
    void reset() { /* no-op (state in fields) */ }

    @Test
    void aop_proxy_and_serialization() throws InterruptedException {
        assertThat(AopUtils.isAopProxy(target)).isTrue();

        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    target.critical(42L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        assertThat(target.counter()).isEqualTo(threads);
        assertThat(target.getMaxObserved()).isEqualTo(1);
    }
}
