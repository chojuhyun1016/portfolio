package org.example.order.core.lock.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.order.core.lock.annotation.DistributedLock;
import org.example.order.core.lock.annotation.DistributedLockT;
import org.example.order.core.lock.key.LockKeyGenerator;
import org.example.order.core.lock.lock.LockCallback;
import org.example.order.core.lock.lock.LockExecutor;
import org.example.order.core.lock.factory.LockExecutorFactory;
import org.example.order.core.lock.factory.LockKeyGeneratorFactory;
import org.example.order.core.lock.service.TransactionalService;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final LockKeyGeneratorFactory keyGeneratorFactory;
    private final LockExecutorFactory lockExecutorFactory;
    private final TransactionalService transactionalService;

    @Around("@annotation(distributedLock)")
    public Object handle(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        LockKeyGenerator keyGenerator = keyGeneratorFactory.getGenerator(distributedLock.keyStrategy());
        String key = keyGenerator.generate(distributedLock.key(), method, joinPoint.getArgs());

        LockExecutor executor = lockExecutorFactory.getExecutor(distributedLock.type());

        try {
            return executor.execute(
                    key,
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    (LockCallback<Object>) () -> {
                        return transactionalService.runWithExistingTransaction(() -> joinPoint.proceed());
                    }
            );
        } catch (Exception e) {
            log.error("Lock execution failed. key={}, type={}, method={}", key, distributedLock.type(), method.getName(), e);
            throw e;
        }
    }

    @Around("@annotation(distributedLockT)")
    public Object handle(ProceedingJoinPoint joinPoint, DistributedLockT distributedLockT) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        LockKeyGenerator keyGenerator = keyGeneratorFactory.getGenerator(distributedLockT.keyStrategy());
        String key = keyGenerator.generate(distributedLockT.key(), method, joinPoint.getArgs());

        LockExecutor executor = lockExecutorFactory.getExecutor(distributedLockT.type());

        try {
            return executor.execute(
                    key,
                    distributedLockT.waitTime(),
                    distributedLockT.leaseTime(),
                    (LockCallback<Object>) () -> {
                        return transactionalService.runWithNewTransaction(() -> joinPoint.proceed());
                    }
            );
        } catch (Exception e) {
            log.error("Lock T execution failed. key={}, type={}, method={}", key, distributedLockT.type(), method.getName(), e);
            throw e;
        }
    }
}
