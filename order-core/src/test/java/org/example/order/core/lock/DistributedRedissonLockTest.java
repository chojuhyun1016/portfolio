package org.example.order.core.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.lock.annotation.DistributedLock;
import org.example.order.core.lock.config.TestRedisConfig;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {DistributedRedissonLockTest.MockLockExecutorConfig.class, TestRedisConfig.class})
@ComponentScan("org.example.order.core") // 실제 구성 스캔
public class DistributedRedissonLockTest {

    private final AtomicInteger counter = new AtomicInteger();

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0.5")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @DistributedLock(key = "'lock-test'", type = "redissonLock", waitTime = 3000, leaseTime = 5000)
    public void criticalSection() {
        int value = counter.incrementAndGet();
        log.info("[criticalSection] Executed by Thread: {}, Value: {}", Thread.currentThread().getName(), value);
        try {
            Thread.sleep(100);
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

        log.info("Final counter value = {}", counter.get());
        assertThat(counter.get()).isEqualTo(threadCount);
    }

    @Configuration
    static class MockLockExecutorConfig {
        @Bean
        public org.example.order.core.lock.key.LockKeyGenerator lockKeyGenerator() {
            return new org.example.order.core.lock.key.SpelLockKeyGenerator();
        }
    }
}
