package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.service.LockService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
    @ComponentScan(basePackages = "org.example.order.core.lock")
    static class TestConfig {
    }

    @Autowired
    private LockService lockService;

    @Test
    @Transactional
    void testConcurrentLocking() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<String>> results = new ArrayList<>();

        log.info("[testConcurrentLocking] 분산락 테스트 시작");

        this.lockService.clear();

        for (int i = 0; i < threadCount; i++) {
            results.add(executorService.submit(() -> {
                try {
                    return lockService.runWithLock("test-key");
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();
        executorService.shutdown();

        List<String> completed = new ArrayList<>();
        for (Future<String> future : results) {
            completed.add(future.get());
        }

        log.info("결과 = {}", completed);

        assertThat(completed).hasSize(threadCount);
    }

    @Test
    @Transactional
    void testConcurrentLockingT() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<String>> results = new ArrayList<>();

        log.info("[testConcurrentLocking T] 분산락 테스트 시작");

        this.lockService.clear();

        for (int i = 0; i < threadCount; i++) {
            results.add(executorService.submit(() -> {
                try {
                    return lockService.runWithLockT("test-key-t");
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();
        executorService.shutdown();

        List<String> completed = new ArrayList<>();
        for (Future<String> future : results) {
            completed.add(future.get());
        }

        log.info("결과 T = {}", completed);

        assertThat(completed).hasSize(threadCount);
    }

    @Test
    void proxyClassCheck() {
        log.info("[PROXY CHECK] isAopProxy = {}", AopUtils.isAopProxy(lockService));
        log.info("[PROXY CHECK] actual class = {}", lockService.getClass());
        log.info("[PROXY CHECK] target class = {}", AopProxyUtils.ultimateTargetClass(lockService));
    }
}
