package org.example.order.core.infra.lock;

import org.example.order.core.IntegrationBoot;
import org.example.order.core.infra.lock.config.LockInfraConfig;
import org.example.order.core.infra.lock.service.LockedServices;
import org.example.order.core.infra.lock.support.RedisTC;
import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedissonLock AOP & 직렬화 보장 통합 테스트
 * - Testcontainers Redis 사용
 * - 오토컨피그 제외 + LockInfraConfig 생성 경로 검증
 * - Fallback RedissonClient로 빈 미등록 레이스 방지
 */
@SpringBootTest(
        classes = IntegrationBoot.class,
        properties = {
                "spring.profiles.active=integration",
                "lock.enabled=true",
                "lock.named.enabled=false",
                "lock.redisson.enabled=true",
                "lock.redisson.wait-time=5000",
                "lock.redisson.lease-time=2500",
                "lock.redisson.retry-interval=100"
        }
)
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@Import({LockInfraConfig.class, RedissonLockAopIT.RedissonClientFallbackConfig.class})
class RedissonLockAopIT extends AbstractIntegrationTest {

    static {
        RedisTC.ensureStarted();
    }

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        RedisTC.register(r);
    }

    @org.springframework.beans.factory.annotation.Autowired
    LockedServices svc;

    ExecutorService pool;

    @BeforeEach
    void setUp() {
        svc.reset();
        pool = Executors.newFixedThreadPool(8);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    @DisplayName("AOP 프록시 적용 확인 - redisson")
    void aopProxyApplied_redisson() {
        assertTrue(AopUtils.isAopProxy(svc), "LockedServices 는 AOP 프록시여야 합니다.");
    }

    @Test
    @DisplayName("RedissonLock + SpEL (REQUIRED): 동시 진입 직렬화")
    void redisson_spel_serializes() throws Exception {
        final int workers = 8;
        final long sleep = 60;
        final long id = 999L;

        List<Future<Integer>> futures = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < workers; i++) {
            futures.add(pool.submit(() -> {
                start.await();

                return svc.redissonSpel(id, sleep);
            }));
        }

        start.countDown();

        for (Future<Integer> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }

        assertEquals(workers, svc.getTotalCalls().get(), "완료 횟수 불일치");
        assertEquals(1, svc.getMaxObserved().get(), "직렬화 실패");
    }

    @Test
    @DisplayName("RedissonLock + Simple (REQUIRES_NEW): 직렬화")
    void redisson_simple_requiresNew_serializes() throws Exception {
        final int workers = 8;
        final long sleep = 50;

        svc.reset();
        List<Future<Integer>> futures = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < workers; i++) {
            futures.add(pool.submit(() -> {
                start.await();

                return svc.redissonSimple_TxNew(sleep);
            }));
        }

        start.countDown();
        for (Future<Integer> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }

        assertEquals(workers, svc.getTotalCalls().get(), "완료 횟수 불일치");
        assertEquals(1, svc.getMaxObserved().get(), "직렬화 실패");
    }

    /**
     * 테스트 전용 Fallback:
     * - LockInfraConfig의 redissonClient 조건이 만족하지 못할 경우 대비
     * - 이미 빈이 있으면 생성하지 않음(ConditionalOnMissingBean)
     */
    @Configuration
    static class RedissonClientFallbackConfig {

        @Bean
        @ConditionalOnMissingBean(RedissonClient.class)
        @ConditionalOnProperty(name = "lock.redisson.enabled", havingValue = "true")
        public RedissonClient redissonClient(Environment env) {
            String addr = env.getProperty("lock.redisson.address");

            if (addr == null || addr.isBlank()) {
                String host = env.getProperty("spring.data.redis.host", "localhost");
                Integer port = env.getProperty("spring.data.redis.port", Integer.class, 6379);
                addr = "redis://" + host + ":" + port;
            }

            Config config = new Config();
            config.useSingleServer().setAddress(addr);

            return Redisson.create(config);
        }
    }
}
