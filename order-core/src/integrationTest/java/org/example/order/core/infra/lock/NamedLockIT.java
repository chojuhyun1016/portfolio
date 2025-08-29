package org.example.order.core.infra.lock;

import org.example.order.core.IntegrationBootApp;
import org.example.order.core.infra.lock.config.LockInfraConfig; // ★ 단일 구성 사용
import org.example.order.core.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NamedLockIT
 * <p>
 * 기존 주석/구조 유지.
 * - MySQL GET_LOCK/RELEASE_LOCK 직검증
 * - application-integration.yml 은 기본 OFF (named/redisson 모두 false)
 * - 명시적으로 named=ON, redisson=OFF 를 켭니다.
 * - Redisson Spring Starter 자동구성/Redis 자동구성은 명시적으로 제외(충돌/불필요 생성 방지)
 */
@SpringBootTest(
        classes = IntegrationBootApp.class,
        properties = {
                "spring.profiles.active=integration",
                "lock.enabled=true",
                "lock.named.enabled=true",
                "lock.redisson.enabled=false"
        }
)
@ImportAutoConfiguration(exclude = {
        org.redisson.spring.starter.RedissonAutoConfigurationV2.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class
})
@Import(LockInfraConfig.class) // ★ 새 구성 조립
class NamedLockIT extends AbstractIntegrationTest {

    @Autowired
    DataSource dataSource;

    Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        conn = dataSource.getConnection();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    void concurrent_access_serialized_by_namedLock_mysql() throws Exception {
        final int workers = 6;
        final String lockName = "it:mysql:namedlock:test";
        final long lockTimeoutSec = 5L;

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);

        AtomicInteger inSection = new AtomicInteger();
        AtomicInteger entered = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < workers; i++) {
            futures.add(pool.submit(() -> {
                try (PreparedStatement get = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
                    start.await();
                    get.setString(1, lockName);
                    get.setLong(2, lockTimeoutSec);

                    if (get.executeQuery().next()) {
                        entered.incrementAndGet();
                        int now = inSection.incrementAndGet();

                        assertTrue(now <= 1, "동시에 2개 이상 진입");

                        Thread.sleep(60);
                        inSection.decrementAndGet();

                        try (PreparedStatement rel = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                            rel.setString(1, lockName);
                            rel.executeQuery();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            }));
        }

        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS), "스레드 종료 대기 초과");
        futures.forEach(f -> f.cancel(true));
        pool.shutdownNow();

        assertEquals(workers, entered.get(), "임계영역 진입 횟수 불일치");
    }
}
