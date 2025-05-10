package org.example.order.core.infra.lock.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.lock.LockCallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랜잭션 래핑을 제공하는 기술적 유틸리티.
 *
 * - 기존 트랜잭션을 이어받아 실행
 * - 새로운 트랜잭션으로 실행
 *
 * infra 레이어의 보조 기술 컴포넌트이며,
 * 비즈니스 로직은 포함하지 않음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalOperator {

    @Transactional(propagation = Propagation.REQUIRED)
    public Object runWithExistingTransaction(LockCallback<Object> callback) throws Throwable {
        long txStart = System.currentTimeMillis();

        try {
            return callback.call();
        } finally {
            long txElapsed = System.currentTimeMillis() - txStart;
            log.info("[TX] 기존 트랜잭션 처리 시간: {}ms", txElapsed);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object runWithNewTransaction(LockCallback<Object> callback) throws Throwable {
        long txStart = System.currentTimeMillis();

        try {
            return callback.call();
        } finally {
            long txElapsed = System.currentTimeMillis() - txStart;
            log.info("[TX - NEW] 새로운 트랜잭션 처리 시간: {}ms", txElapsed);
        }
    }
}
