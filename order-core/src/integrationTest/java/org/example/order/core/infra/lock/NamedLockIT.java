package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.TestBootApp;
import org.example.order.core.infra.lock.config.LockManualConfig;     // 프로덕션 구성
import org.example.order.core.infra.lock.config.NamedLockAutoConfig; // 프로덕션 구성(프로퍼티 바인딩)
import org.example.order.core.infra.lock.support.MysqlContainerSupport; // 테스트 컨테이너
import org.example.order.core.infra.lock.support.TestNamedLockTarget; // 테스트용 namedLock 대상
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
 * MySQL(Testcontainers) + namedLock 통합 테스트
 * - LockManualConfig + NamedLockAutoConfig 그대로 사용
 * - 컨테이너 MySQL 연결정보는 MysqlContainerSupport 가 동적 주입
 * - TestNamedLockTarget(namedLock)으로 직렬화 보장 검증
 */
@Slf4j
@SpringBootTest(classes = TestBootApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test-integration")
@Import({
        LockManualConfig.class,
        NamedLockAutoConfig.class
})
class NamedLockIT extends MysqlContainerSupport {

    @Autowired
    private TestNamedLockTarget target;

    @BeforeEach
    void setUp() {
        target.reset();
    }

    @Test
    void concurrent_access_serialized_by_namedLock_mysql() throws InterruptedException {
        int threadCount = 6;
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
