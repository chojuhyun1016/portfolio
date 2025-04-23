package org.example.order.core.infra.lock;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.service.LockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = DistributedNamedLockTest.TestConfig.class)
@ActiveProfiles("test")
class DistributedNamedLockTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = "org.example.order.core.infra.lock")
    static class TestConfig {}

    @Autowired
    private LockService lockService;

    @Test
    void testConcurrentLocking() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        log.info("[testConcurrentLocking] 분산락 테스트 시작");
        lockService.clear();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                latch.await();
                try {
                    return lockService.runWithLock("test-key");
                } catch (Exception e) {
                    log.warn("[test] 예외 발생: {}", e.getMessage());
                    return "LOCK_FAIL";
                }
            }));
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(20, TimeUnit.SECONDS);

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        log.info("결과 = {}", results);
        long successCount = results.stream().filter(s -> !"LOCK_FAIL".equals(s)).count();
        assertThat(successCount).isGreaterThan(0);
    }

    @Test
    void testConcurrentLockingT() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        log.info("[testConcurrentLockingT] 분산락 테스트 시작");
        lockService.clear();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                latch.await();
                try {
                    return lockService.runWithLockT("test-key-t");
                } catch (Exception e) {
                    log.warn("[test T] 예외 발생: {}", e.getMessage());
                    return "LOCK_FAIL";
                }
            }));
        }

        latch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(20, TimeUnit.SECONDS);

        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        log.info("결과 T = {}", results);
        long successCount = results.stream().filter(s -> !"LOCK_FAIL".equals(s)).count();
        assertThat(successCount).isGreaterThan(0);
    }
}
