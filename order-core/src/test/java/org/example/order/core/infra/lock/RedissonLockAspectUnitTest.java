// src/test/java/org/example/order/core/infra/lock/RedissonLockAspectUnitTest.java

package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.config.LockCoreTestSlice;
import org.example.order.core.infra.lock.config.RedissonMockEnabledConfig;
import org.example.order.core.infra.lock.support.DummyLockTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
@SpringBootTest(classes = RedissonLockAspectUnitTest.Boot.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test-unit")
@TestPropertySource(properties = {
        // 락 코어 슬라이스를 로드하기 위한 스위치
        "lock.enabled=true",
        // 이번 테스트는 redisson 타입을 사용(실제 레디슨이 아니라 InMemory 모의 실행기)
        "lock.redisson.enabled=true",
        "lock.named.enabled=false",
        // 진짜 Redis 자동설정 비활성화(테스트에서 불필요)
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@Import({
        LockCoreTestSlice.class,      // AOP/Factory/KeyGenerators/TxOperator
        RedissonMockEnabledConfig.class // 이름이 "redissonLock" 인 InMemory LockExecutor 제공
})
class RedissonLockAspectUnitTest {

    /**
     * 최소한의 부트스트랩용 설정(테스트 컨텍스트 전용).
     * 불필요한 컴포넌트 스캔 없이 필요한 빈만 등록합니다.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Boot {
        @Bean
        DummyLockTarget dummyLockTarget() {
            return new DummyLockTarget();
        }
    }

    private final DummyLockTarget target;

    // ★ 권장: 생성자 주입 + @Autowired (JUnit 5 ParameterResolver 오류 방지)
    @Autowired
    RedissonLockAspectUnitTest(DummyLockTarget target) {
        this.target = target;
    }

    @BeforeEach
    void setUp() {
        target.reset();
    }

    @Test
    void concurrent_access_is_serialized() throws InterruptedException {
        int threadCount = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    target.criticalSection(); // @DistributedLock 적용 메서드(직렬화 기대)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        // 스레드마다 1회씩 수행되었는지
        assertThat(target.getCounter()).isEqualTo(threadCount);
        // 동시에 임계영역에 들어간 최대 스레드 수가 1인지(직렬화 보장)
        assertThat(target.getMaxObserved()).isEqualTo(1);
    }
}
