package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.annotation.DistributedLock;
import org.example.order.core.infra.lock.key.LockKeyGenerator;
import org.example.order.core.infra.lock.config.TestRedisConfig;
import org.example.order.core.infra.lock.key.impl.SpelLockKeyGenerator;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redisson(레디스 기반) 동시성 테스트
 * - Testcontainers Redis + TestRedisConfig 으로 RedissonClient 주입
 * - lock.enabled=true, lock.redisson.enabled=true 로 RedissonLock 실행기 활성화
 * - NamedLock은 명시적으로 비활성화
 */
@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test") // 🔸 프로필 활성화(= TestRedisConfig 활성)
@ContextConfiguration(classes = {DistributedRedissonLockTest.MockLockExecutorConfig.class, TestRedisConfig.class})
@ComponentScan("org.example.order.core")
@TestPropertySource(properties = {
        "lock.enabled=true",
        "lock.redisson.enabled=true",
        "lock.named.enabled=false"
})
public class DistributedRedissonLockTest {

    private final AtomicInteger counter = new AtomicInteger();

    @DistributedLock(key = "'lock-test'", type = "redissonLock", waitTime = 3000, leaseTime = 5000)
    public void criticalSection() {
        int value = counter.incrementAndGet();
        log.info("[criticalSection] Thread={} | Value={}", Thread.currentThread().getName(), value);
        try {
            Thread.sleep(100); // 경합 유도
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @RepeatedTest(1)
    public void testConcurrentLocking() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    criticalSection();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        log.info("[Redisson] Final counter value = {}", counter.get());
        assertThat(counter.get()).isEqualTo(threadCount);
    }

    @Configuration
    static class MockLockExecutorConfig {
        @Bean
        public LockKeyGenerator lockKeyGenerator() {
            return new SpelLockKeyGenerator();
        }
    }
}
