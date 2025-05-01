package org.example.order.core.infra.lock;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = DistributedTransactionTest.TestConfig.class,
        properties = {
                "spring.cloud.gateway.enabled=false",
                "spring.cloud.gateway.redis.enabled=false",
                "spring.main.web-application-type=servlet"
        }
)
@ActiveProfiles("test")
class DistributedTransactionTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {
                    org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class
            }
    )
    @Execution(ExecutionMode.SAME_THREAD)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = "org.example.order.core.infra.lock")
    static class TestConfig {}

    @Autowired
    private LockService lockService;

    @Test
    @Transactional
    void testConcurrentLocking() throws ExecutionException, InterruptedException {
        int count = 10;
        String outerTxId = TransactionSynchronizationManager.getCurrentTransactionName();
        log.info("[testConcurrentLocking] Outer Transaction ID: {}", outerTxId);

        List<String> txIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String innerTxId = lockService.runWithLock("test-key");
            txIds.add(innerTxId);
        }

        txIds.forEach(id -> log.info("[LOCK] Returned TX ID: {}", id));

        assertThat(txIds).allMatch(tx -> tx.equals(outerTxId));
    }

    @Test
    @Transactional
    void testConcurrentLockingT() throws ExecutionException, InterruptedException {
        int count = 10;
        String outerTxId = TransactionSynchronizationManager.getCurrentTransactionName();
        log.info("[testConcurrentLockingT] Outer Transaction ID: {}", outerTxId);

        this.lockService.clear();

        List<String> txIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String innerTxId = lockService.runWithLockT("test-key-t");
            txIds.add(innerTxId);
        }

        txIds.forEach(id -> log.info("[LOCK T] Returned TX ID: {}", id));

        assertThat(txIds).noneMatch(tx -> tx.equals(outerTxId));
    }
}
