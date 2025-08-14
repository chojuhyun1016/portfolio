package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.config.TestMySqlConfig;
import org.example.order.core.infra.lock.service.LockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 트랜잭션 전파 테스트
 * - REQUIRED: 동일 트랜잭션 ID 유지
 * - REQUIRES_NEW: 상위와 다른 트랜잭션 ID
 * - NamedLock 사용 → MySQL Testcontainers 로 GET_LOCK/RELEASE_LOCK 제공
 */
@Slf4j
@SpringBootTest(
        classes = DistributedTransactionTest.TestConfig.class,
        properties = {
                "spring.cloud.gateway.enabled=false",
                "spring.cloud.gateway.redis.enabled=false",
                "spring.main.web-application-type=servlet"
        }
)
@ContextConfiguration(classes = TestMySqlConfig.class) // 🔸 MySQL DataSource 주입
@TestPropertySource(properties = {
        "lock.enabled=true",
        "lock.named.enabled=true",
        "lock.redisson.enabled=false"
})
@ActiveProfiles("test")
class DistributedTransactionTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class}
    )
    @Execution(ExecutionMode.SAME_THREAD)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = "org.example.order.core.infra.lock")
    static class TestConfig {
    }

    @Autowired
    private LockService lockService;

    @Test
    @Transactional
    void testConcurrentLocking() throws ExecutionException, InterruptedException {
        int count = 10;
        String outerTxId = TransactionSynchronizationManager.getCurrentTransactionName();
        log.info("[REQUIRED] Outer TX ID: {}", outerTxId);

        List<String> txIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String innerTxId = lockService.runWithLock("test-key");
            txIds.add(innerTxId);
        }

        txIds.forEach(id -> log.info("[REQUIRED] Returned TX ID: {}", id));
        assertThat(txIds).allMatch(tx -> tx.equals(outerTxId));
    }

    @Test
    @Transactional
    void testConcurrentLockingT() throws ExecutionException, InterruptedException {
        int count = 10;
        String outerTxId = TransactionSynchronizationManager.getCurrentTransactionName();
        log.info("[REQUIRES_NEW] Outer TX ID: {}", outerTxId);

        lockService.clear();

        List<String> txIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String innerTxId = lockService.runWithLockT("test-key-t");
            txIds.add(innerTxId);
        }

        txIds.forEach(id -> log.info("[REQUIRES_NEW] Returned TX ID: {}", id));
        assertThat(txIds).noneMatch(tx -> tx.equals(outerTxId));
    }
}
