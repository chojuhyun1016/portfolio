package org.example.order.core.infra.lock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.lock.lock.LockCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionalService {

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
            log.info("[TX - T] 기존 트랜잭션 처리 시간: {}ms", txElapsed);
        }
    }
}
