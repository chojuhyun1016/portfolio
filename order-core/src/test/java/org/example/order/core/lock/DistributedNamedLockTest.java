package org.example.order.core.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.lock.annotation.DistributedLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class DistributedNamedLockTest {

    @Autowired
    private NamedLockTestService namedLockTestService;

    @Test
    void testConcurrentLocking() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executorService.submit(() -> {
                try {
                    return namedLockTestService.runWithLock("test-key");
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(); // 모든 스레드 완료 대기
        executorService.shutdown();

        List<String> completed = new ArrayList<>();
        for (Future<String> future : results) {
            completed.add(future.get());
        }

        log.info("결과 = {}", completed);
        assertThat(completed).hasSize(threadCount);

        // 순서 검증
        for (int i = 0; i < completed.size(); i++) {
            assertThat(completed.get(i)).isEqualTo(String.valueOf(i + 1));
        }
    }

    @Service
    public static class NamedLockTestService {

        private final AtomicInteger sequence = new AtomicInteger();

        @DistributedLock(key = "#key", type = "namedLock", waitTime = 5000, leaseTime = 3000)
        public String runWithLock(String key) throws InterruptedException {
            int order = sequence.incrementAndGet();
            log.info("[LOCK] 진입 - key={}, order={}", key, order);
            Thread.sleep(500); // 처리 시간
            log.info("[LOCK] 종료 - order={}", order);
            return String.valueOf(order);
        }
    }

    @Configuration
    @Import(NamedLockTestService.class)
    public static class NamedLockTestConfig {
        // 필요한 경우 다른 빈도 여기서 등록 가능
    }
}
