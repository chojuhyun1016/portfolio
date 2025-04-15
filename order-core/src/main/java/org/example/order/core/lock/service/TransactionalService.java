package org.example.order.core.lock.service;

import lombok.RequiredArgsConstructor;
import org.example.order.core.lock.lock.LockCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionalService {

    @Transactional(propagation = Propagation.REQUIRES_NEW) // `REQUIRES_NEW` 트랜잭션 전파 방식
    public Object runWithNewTransaction(LockCallback<Object> callback) throws Throwable {
        return callback.call();  // 새로운 트랜잭션에서 실행
    }

    @Transactional(propagation = Propagation.REQUIRED) // 기존 트랜잭션과 연결하여 실행
    public Object runWithExistingTransaction(LockCallback<Object> callback) throws Throwable {
        return callback.call();  // 기존 트랜잭션에서 실행
    }
}
