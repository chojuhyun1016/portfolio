package org.example.order.core.infra.lock;

import org.example.order.core.IntegrationBoot;
import org.example.order.core.infra.lock.config.LockInfraConfig;
import org.example.order.core.infra.lock.service.LockedServices;
import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NamedLock AOP & 직렬화 보장 통합 테스트
 * - MySQL GET_LOCK/RELEASE_LOCK 기반 직렬화 보장 검증
 * - 오토컨피그 제외 + LockInfraConfig 조립
 * - (중요) NamedLock 경로는 스레드당 DB 커넥션 2개 사용 → Hikari 풀 여유 필요
 */
@SpringBootTest(
        classes = IntegrationBoot.class,
        properties = {
                "spring.profiles.active=integration",
                "jpa.enabled=false",
                "lock.enabled=true",
                "lock.named.enabled=true",
                "lock.redisson.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.datasource.hikari.maximum-pool-size=40",
                "spring.datasource.hikari.connection-timeout=45000"
        }
)
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@Import(LockInfraConfig.class)
class NamedLockAopIT extends AbstractIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    LockedServices svc;

    ExecutorService pool;

    @BeforeEach
    void setUp() {
        svc.reset();
        pool = Executors.newFixedThreadPool(12);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    @DisplayName("AOP 프록시 적용 확인 - named")
    void aopProxyApplied_named() {
        assertTrue(AopUtils.isAopProxy(svc), "LockedServices 는 AOP 프록시여야 합니다.");
    }

    @Test
    @DisplayName("NamedLock + SpEL (REQUIRED): 동시 진입 직렬화")
    void named_spel_serializes() throws Exception {
        final int workers = 12;
        final long sleep = 50L;
        final long orderId = 42L;

        List<Future<Integer>> futures = new ArrayList<>(workers);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < workers; i++) {
            futures.add(pool.submit(() -> {
                start.await();

                return svc.namedSpel(orderId, sleep);
            }));
        }

        start.countDown();

        for (Future<Integer> f : futures) {
            assertNotNull(f.get(60, TimeUnit.SECONDS));
        }

        assertEquals(workers, svc.getTotalCalls().get(), "완료 횟수 불일치");
        assertEquals(1, svc.getMaxObserved().get(), "직렬화 실패");
    }

    @Test
    @DisplayName("NamedLock + SHA256 (REQUIRES_NEW): 직렬화")
    void named_sha256_requiresNew_serializes() throws Exception {
        final int workers = 12;
        final long sleep = 50L;

        svc.reset();

        List<Future<Integer>> futures = new ArrayList<>(workers);
        CountDownLatch start = new CountDownLatch(1);

        for (int i = 0; i < workers; i++) {
            futures.add(pool.submit(() -> {
                start.await();

                return svc.namedSha256_TxNew("payload", sleep);
            }));
        }

        start.countDown();

        for (Future<Integer> f : futures) {
            assertNotNull(f.get(60, TimeUnit.SECONDS));
        }

        assertEquals(workers, svc.getTotalCalls().get(), "완료 횟수 불일치");
        assertEquals(1, svc.getMaxObserved().get(), "직렬화 실패");
    }
}
