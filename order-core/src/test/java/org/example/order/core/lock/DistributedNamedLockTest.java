package org.example.order.core.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.lock.service.LockService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    void testConcurrentLocking() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executorService.submit(() -> {
                try {
                    String result = lockService.runWithLock("test-key");
                    log.info("[RESULT] get from Future: {}", result);
                    return result;
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();
        executorService.shutdown();

        List<Integer> completed = new ArrayList<>();
        for (Future<String> future : results) {
            completed.add(Integer.parseInt(future.get()));
        }

        log.info("결과 = {}", completed);

        // ✅ 변경된 부분: 정렬하여 검증
        List<Integer> sorted = new ArrayList<>(completed);
        Collections.sort(sorted);

        assertThat(completed).hasSize(threadCount);
        assertThat(sorted).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void proxyClassCheck() {
        log.info("[PROXY CHECK] isAopProxy = {}", AopUtils.isAopProxy(lockService));
        log.info("[PROXY CHECK] actual class = {}", lockService.getClass());
        log.info("[PROXY CHECK] target class = {}", AopProxyUtils.ultimateTargetClass(lockService));
    }
}
