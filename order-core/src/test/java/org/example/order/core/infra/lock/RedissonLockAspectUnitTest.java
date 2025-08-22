package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.TestBootApp;
import org.example.order.core.infra.lock.config.LockManualConfig;          // 원본 구성 사용 (수정 없음)
import org.example.order.core.infra.lock.config.RedissonMockUnitConfig;   // 테스트용 인메모리 redissonLock 빈
import org.example.order.core.infra.lock.support.DummyLockTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = TestBootApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test-unit")
@TestPropertySource(properties = {
        "lock.enabled=true",
        "lock.redisson.enabled=false",
        "lock.named.enabled=false",
        // 클래스패스에 ‘실존’하는 것만 제외
        "spring.autoconfigure.exclude=" +
                "org.redisson.spring.starter.RedissonAutoConfigurationV2," +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@Import({
        LockManualConfig.class,        // 원본 Lock 구성(Aspect/Factory 등)을 그대로 사용
        RedissonMockUnitConfig.class   // redissonLock 빈을 인메모리로 대체 주입 (원본 미변경)
})
class RedissonLockAspectUnitTest {

    @Autowired
    private DummyLockTarget target; // 스프링 빈을 통해서만 AOP가 적용됨

    @BeforeEach
    void setUp() {
        target.reset();
    }

    @Test
    void concurrent_access_is_serialized() throws InterruptedException {
        int threadCount = 5;
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
        assertThat(target.getMaxObserved()).isEqualTo(1); // 직렬 보장
    }
}
