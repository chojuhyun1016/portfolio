package org.example.order.core.infra.lock;

import org.example.order.core.infra.lock.config.LockCoreTestSlice;
import org.example.order.core.infra.lock.config.RedissonMockEnabledConfig;
import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SelfInvocationBypassUnitTest.Boot.class)
@ActiveProfiles("test-unit")
// - LockCoreTestSlice / RedissonMockEnabledConfig 가 @ConditionalOnProperty 에 걸리므로
//   테스트 프로퍼티로 명시적으로 활성화한다.
@TestPropertySource(properties = {
        "lock.enabled=true",
        "lock.redisson.enabled=true",
        "lock.named.enabled=false",
        // 외부 Redis 오토컨피그는 막고, 테스트 슬라이스의 인메모리 락을 사용
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@Import({LockCoreTestSlice.class, RedissonMockEnabledConfig.class})
class SelfInvocationBypassUnitTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Boot {
        @Bean
        ProxyTarget proxyTarget() {
            return new ProxyTarget();
        }
    }

    static class ProxyTarget {
        private final java.util.concurrent.atomic.AtomicInteger concurrent = new java.util.concurrent.atomic.AtomicInteger(0);
        private final java.util.concurrent.atomic.AtomicInteger maxObserved = new java.util.concurrent.atomic.AtomicInteger(0);
        private final java.util.concurrent.atomic.AtomicInteger total = new java.util.concurrent.atomic.AtomicInteger(0);

        @org.example.order.core.infra.lock.annotation.DistributedLock(
                key = "'unit-self:' + 'x'",
                type = "redissonLock",
                waitTime = 500,
                leaseTime = 1000
        )
        public void critical() {
            int now = concurrent.incrementAndGet();
            maxObserved.accumulateAndGet(now, Math::max);
            try {
                total.incrementAndGet();
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                concurrent.decrementAndGet();
            }
        }

        int maxObserved() {
            return maxObserved.get();
        }

        int total() {
            return total.get();
        }
    }

    @Autowired
    ProxyTarget target;

    @Test
    void direct_proxy_call_to_annotated_method_is_serialized() throws Exception {
        // AOP 프록시가 활성화되었는지 확인
        assertThat(AopUtils.isAopProxy(target)).isTrue();

        int workers = 7;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < workers; i++) {
            pool.submit(() -> {
                start.await();
                target.critical(); // 반드시 프록시를 통해 호출되며 직렬화되어야 함
                return null;
            });
        }

        start.countDown();

        pool.shutdown();
        // 타임아웃 여유를 조금 더 줘서 CI 환경 플래키 개선
        boolean finished = pool.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).as("executor finished").isTrue();

        assertThat(target.total()).isEqualTo(workers);
        assertThat(target.maxObserved()).isEqualTo(1); // 직렬화 보장
    }
}
