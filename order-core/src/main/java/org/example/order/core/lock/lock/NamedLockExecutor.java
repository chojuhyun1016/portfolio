package org.example.order.core.lock.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.exception.ExceptionUtils;
import org.example.order.core.lock.config.NamedLockProperties;
import org.example.order.core.lock.exception.LockAcquisitionException;
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
        int retryInterval = namedLockProperties.getRetryInterval(); // ex: 100ms
        int maxRetries = waitMillis / retryInterval;

        String getLockSql = "SELECT GET_LOCK(?, ?)";
        String releaseLockSql = "SELECT RELEASE_LOCK(?)";

        long start = System.currentTimeMillis();
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;
            try {
                Boolean locked = jdbcTemplate.queryForObject(getLockSql, Boolean.class, key, retryInterval / 1000.0);
                if (Boolean.TRUE.equals(locked)) {
                    log.debug("Acquired named lock. key={}, attempt={}, waited={}ms", key, attempt, System.currentTimeMillis() - start);

                    try {
                        return callback.call();
                    } finally {
                        try {
                            Boolean released = jdbcTemplate.queryForObject(releaseLockSql, Boolean.class, key);
                            if (!Boolean.TRUE.equals(released)) {
                                log.warn("Failed to release lock properly. key={}, result={}", key, released);
                            }
                            log.debug("Released named lock. key={}", key);
                        } catch (Exception e) {
                            log.error("Failed to release named lock. key={}, error={}", key, e.getMessage(), e);
                        }
                    }
                } else {
                    log.debug("Lock attempt failed. key={}, attempt={}, retrying...", key, attempt);
                }

                TimeUnit.MILLISECONDS.sleep(retryInterval);
            } catch (DataAccessException e) {
                SQLException sqlEx = ExceptionUtils.findSQLException(e);
                log.error("""
                    SQL error during named lock
                    ├─ key         : {}
                    ├─ attempt     : {}
                    ├─ SQLState    : {}
                    ├─ ErrorCode   : {}
                    └─ RootMessage : {}
                    """,
                        key,
                        attempt,
                        sqlEx != null ? sqlEx.getSQLState() : "N/A",
                        sqlEx != null ? sqlEx.getErrorCode() : "N/A",
                        sqlEx != null ? sqlEx.getMessage() : e.getMessage(),
                        e
                );
                throw new LockAcquisitionException("SQL error during named lock for key: " + key, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("Thread interrupted during named lock wait. key: " + key, e);
            } catch (Exception e) {
                log.error("Unexpected error during named lock execution. key={}, error={}", key, e.getMessage(), e);
                throw new LockAcquisitionException("Unexpected error during named lock for key: " + key, e);
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        throw new LockAcquisitionException("Failed to acquire named lock for key: " + key + " after " + elapsed + "ms and " + attempt + " attempts.");
    }
}
