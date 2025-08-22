package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.TestBootApp;
import org.example.order.core.infra.lock.config.LockManualConfig;          // 프로덕션 구성
import org.example.order.core.infra.lock.config.RedissonLockAutoConfig;   // 프로덕션 구성
import org.example.order.core.infra.lock.support.RedisContainerSupport;   // 테스트 컨테이너
import org.example.order.core.infra.lock.support.DummyLockTarget;         // 프로덕션 테스트용 대상(이미 redissonLock 사용)
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis(Testcontainers) + redissonLock 통합 테스트
 * - LockManualConfig/RedissonLockAutoConfig 를 그대로 사용
 * - 도커 컨테이너 주소는 RedisContainerSupport 가 동적으로 주입
 * - DummyLockTarget(프로덕션, redissonLock 사용)으로 직렬화 보장 검증
 */
@Slf4j
@SpringBootTest(classes = TestBootApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test-integration")
@Import({
        LockManualConfig.class,
        RedissonLockAutoConfig.class
})
class RedissonLockIT extends RedisContainerSupport {

    @Autowired
    private DummyLockTarget target; // 스프링 빈을 통해 AOP 적용

    @BeforeEach
    void setUp() {
        target.reset();
    }

    @Test
    void concurrent_access_serialized_by_redisson() throws InterruptedException {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try { target.criticalSection(); }
                finally { latch.countDown(); }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(target.getCounter()).isEqualTo(threadCount);
        assertThat(target.getMaxObserved())
                .as("동시 구간이 1을 넘지 않아야 직렬화가 보장됨")
                .isEqualTo(1);
    }
}
