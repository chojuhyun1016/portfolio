package org.example.order.core.lock.lock.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.exception.ExceptionUtils;
import org.example.order.core.lock.config.NamedLockProperties;
import org.example.order.core.lock.exception.LockAcquisitionException;
import org.example.order.core.lock.lock.LockCallback;
import org.example.order.core.lock.lock.LockExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component("namedLock")
@RequiredArgsConstructor
public class NamedLockExecutor implements LockExecutor {

    private final NamedLockProperties namedLockProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
        int waitMillis = (int) (waitTime > 0 ? waitTime : namedLockProperties.getWaitTime());
        int retryInterval = namedLockProperties.getRetryInterval();
        int maxRetries = waitMillis / retryInterval;

        String getLockSql = "SELECT GET_LOCK(?, ?)";
        String releaseLockSql = "SELECT RELEASE_LOCK(?)";

        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Boolean locked = jdbcTemplate.queryForObject(getLockSql, Boolean.class, key, retryInterval / 1000.0);

                if (Boolean.TRUE.equals(locked)) {
                    log.debug("Acquired named lock. key={}, attempt={}, waited={}ms", key, attempt, System.currentTimeMillis() - startTime);
                    try {
                        return callback.call();
                    } finally {
                        releaseLock(key, releaseLockSql);
                    }
                }

                log.debug("Named lock attempt failed. key={}, attempt={}, retrying...", key, attempt);
                TimeUnit.MILLISECONDS.sleep(retryInterval);
            } catch (DataAccessException e) {
                SQLException sqlEx = ExceptionUtils.findSQLException(e);

                log.error("""
                    Named lock SQL error
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
                throw new LockAcquisitionException("Named lock interrupted. key=" + key, e);
            } catch (Exception e) {
                log.error("Unexpected error during named lock. key={}, error={}", key, e.getMessage(), e);
                throw new LockAcquisitionException("Unexpected error during named lock. key=" + key, e);
            }
        }

        long totalElapsed = System.currentTimeMillis() - startTime;
        throw new LockAcquisitionException("Named lock failed for key=" + key + " after " + totalElapsed + "ms");
    }

    private void releaseLock(String key, String releaseSql) {
        try {
            Boolean released = jdbcTemplate.queryForObject(releaseSql, Boolean.class, key);

            if (!Boolean.TRUE.equals(released)) {
                log.warn("Named lock not properly released. key={}, result={}", key, released);
            } else {
                log.debug("Released named lock. key={}", key);
            }
        } catch (Exception e) {
            log.error("Failed to release named lock. key={}, error={}", key, e.getMessage(), e);
        }
    }
}
