package org.example.order.core.infra.lock.lock.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.exception.ExceptionUtils;
import org.example.order.core.infra.lock.config.NamedLockProperties;
import org.example.order.core.infra.lock.exception.LockAcquisitionException;
import org.example.order.core.infra.lock.lock.LockCallback;
import org.example.order.core.infra.lock.lock.LockExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("namedLock")
@RequiredArgsConstructor
public class NamedLockExecutor implements LockExecutor {

    private final NamedLockProperties namedLockProperties;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    @Override
    public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
        int waitMillis = (int) (waitTime > 0 ? waitTime : namedLockProperties.getWaitTime());
        int retryInterval = namedLockProperties.getRetryInterval();
        int maxRetries = waitMillis / retryInterval;

        final String getLockSql = "SELECT GET_LOCK(?, ?)";
        final String releaseLockSql = "SELECT RELEASE_LOCK(?)";
        final long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                double timeoutSeconds = retryInterval / 1000.0;

                boolean locked = executeInNewTransaction(() -> jdbcTemplate.queryForObject(getLockSql, Boolean.class, key, timeoutSeconds));

                if (locked) {
                    long acquiredAt = System.currentTimeMillis();
                    log.info("[LOCK] 획득 성공 | key={} | attempt={} | elapsed={}ms", key, attempt, acquiredAt - startTime);
                    try {
                        return callback.call();
                    } finally {
                        executeInNewTransaction(() -> {
                            releaseLock(key, releaseLockSql);
                            return null;
                        });
                    }
                }

                log.debug("[LOCK] 획득 실패 | key={} | attempt={} | 재시도 대기중...", key, attempt);
                TimeUnit.MILLISECONDS.sleep(retryInterval);
            } catch (DataAccessException e) {
                SQLException sqlEx = ExceptionUtils.findSQLException(e);
                log.error("""
                    [LOCK] SQL 예외 발생
                    ├─ key       : {}
                    ├─ attempt   : {}
                    ├─ SQLState  : {}
                    ├─ ErrorCode : {}
                    └─ Message   : {}
                    """, key, attempt,
                        sqlEx != null ? sqlEx.getSQLState() : "N/A",
                        sqlEx != null ? sqlEx.getErrorCode() : "N/A",
                        sqlEx != null ? sqlEx.getMessage() : e.getMessage(),
                        e
                );
                throw new LockAcquisitionException("Named lock SQL error. key=" + key, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[LOCK] 쓰레드 인터럽트 발생 | key={} | attempt={}", key, attempt);
                throw new LockAcquisitionException("Named lock interrupted. key=" + key, e);
            } catch (Exception e) {
                log.error("[LOCK] 예기치 못한 예외 발생 | key={} | attempt={} | message={}", key, attempt, e.getMessage(), e);
                throw new LockAcquisitionException("Unexpected error during named lock. key=" + key, e);
            }
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        log.error("[LOCK] 최종 실패 | key={} | totalElapsed={}ms", key, totalElapsed);
        throw new LockAcquisitionException("Named lock failed for key=" + key + " after " + totalElapsed + "ms");
    }

    private void releaseLock(String key, String releaseSql) {
        try {
            Boolean released = jdbcTemplate.queryForObject(releaseSql, Boolean.class, key);
            if (!Boolean.TRUE.equals(released)) {
                log.warn("[LOCK] 해제 실패 | key={} | result={}", key, released);
            } else {
                log.info("[LOCK] 해제 성공 | key={}", key);
            }
        } catch (Exception e) {
            log.error("[LOCK] 해제 중 예외 발생 | key={} | error={}", key, e.getMessage(), e);
        }
    }

    /**
     * 별도의 트랜잭션 경계에서 실행
     */
    private <T> T executeInNewTransaction(TransactionCallback<T> action) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            T result = action.execute();
            transactionManager.commit(status);
            return result;
        } catch (RuntimeException | Error e) {
            transactionManager.rollback(status);
            throw e;
        } catch (Throwable t) {
            transactionManager.rollback(status);
            throw new RuntimeException(t);
        }
    }

    @FunctionalInterface
    interface TransactionCallback<T> {
        T execute() throws Throwable;
    }
}
