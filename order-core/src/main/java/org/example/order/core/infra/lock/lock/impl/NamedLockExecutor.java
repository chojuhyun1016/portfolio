package org.example.order.core.infra.lock.lock.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.config.NamedLockProperties;
import org.example.order.core.infra.lock.exception.LockAcquisitionException;
import org.example.order.core.infra.lock.lock.LockCallback;
import org.example.order.core.infra.lock.lock.LockExecutor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
@Component("namedLock")
@RequiredArgsConstructor
public class NamedLockExecutor implements LockExecutor {

    private static final String GET_LOCK_SQL = "SELECT GET_LOCK(?, ?)";
    private static final String RELEASE_LOCK_SQL = "SELECT RELEASE_LOCK(?)";
    private static final double GET_LOCK_TIMEOUT_SECONDS = 2.0; // 락 1회 시도 제한 시간 (초)

    private final NamedLockProperties namedLockProperties;
    private final DataSource dataSource;

    @Override
    public Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable {
        int retryInterval = namedLockProperties.getRetryInterval();
        int waitMillis = (int) (waitTime > 0 ? waitTime : namedLockProperties.getWaitTime());
        int maxRetries = waitMillis / retryInterval;

        long startTime = System.currentTimeMillis();
        Connection lockConnection = null;
        boolean locked = false;

        try {
            lockConnection = DataSourceUtils.getConnection(dataSource);

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try (PreparedStatement stmt = lockConnection.prepareStatement(GET_LOCK_SQL)) {
                    stmt.setString(1, key);
                    stmt.setDouble(2, GET_LOCK_TIMEOUT_SECONDS);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            boolean success = rs.getBoolean(1);
                            if (success) {
                                long elapsed = System.currentTimeMillis() - startTime;
                                log.debug("[LOCK] 획득 성공 | key={} | attempt={} | elapsed={}ms", key, attempt, elapsed);
                                locked = true;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[LOCK] DB 예외 (획득) | key={} | attempt={} | error={}", key, attempt, e.getMessage(), e);
                }

                Thread.sleep(retryInterval);
            }

            if (!locked) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("[LOCK] 최종 실패 | key={} | totalElapsed={}ms", key, elapsed);
                throw new LockAcquisitionException("Named lock failed for key=" + key);
            }

            Object result;
            long callStart = System.currentTimeMillis();
            try {
                result = callback.call();
                log.debug("[LOCK] 처리 완료 | key={} | taskElapsed={}ms", key, System.currentTimeMillis() - callStart);
            } catch (Throwable t) {
                log.error("[LOCK] callback 예외 | key={} | error={}", key, t.getMessage(), t);
                throw t;
            } finally {
                try {
                    releaseLock(key, lockConnection);
                } catch (Exception e) {
                    log.error("[LOCK] 해제 중 예외 | key={} | error={}", key, e.getMessage(), e);
                }
            }

            return result;

        } catch (Throwable t) {
            log.error("[LOCK] 실행 예외 | key={} | error={}", key, t.getMessage(), t);
            throw t;
        } finally {
            if (lockConnection != null) {
                try {
                    DataSourceUtils.releaseConnection(lockConnection, dataSource);
                } catch (Exception e) {
                    log.warn("[LOCK] 커넥션 해제 실패 | key={} | error={}", key, e.getMessage(), e);
                }
            }
            long totalElapsed = System.currentTimeMillis() - startTime;
            log.debug("[LOCK] 전체 수행 종료 | key={} | totalElapsed={}ms", key, totalElapsed);
        }
    }

    private void releaseLock(String key, Connection connection) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(RELEASE_LOCK_SQL)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getBoolean(1)) {
                    log.debug("[LOCK] 해제 성공 | key={}", key);
                } else {
                    log.warn("[LOCK] 해제 실패 | key={} | result=false", key);
                }
            }
        }
    }
}
