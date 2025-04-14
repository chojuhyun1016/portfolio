package org.example.order.core.lock.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.order.core.lock.annotation.DistributedLock;
import org.example.order.core.lock.key.LockKeyGenerator;
import org.example.order.core.lock.lock.LockCallback;
import org.example.order.core.lock.lock.LockExecutor;
import org.example.order.core.lock.support.LockExecutorFactory;
import org.example.order.core.lock.support.LockKeyGeneratorFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final LockKeyGeneratorFactory keyGeneratorFactory; // ✅ 변경
    private final LockExecutorFactory lockExecutorFactory;

    @Around("@annotation(distributedLock)")
    public Object handle(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        LockKeyGenerator keyGenerator = keyGeneratorFactory.getGenerator(distributedLock.keyStrategy()); // ✅ 전략 선택
        String key = keyGenerator.generate(distributedLock.key(), method, joinPoint.getArgs());

        LockExecutor executor = lockExecutorFactory.getExecutor(distributedLock.type());

        try {
            return executor.execute(
                    key,
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    (LockCallback<Object>) () -> joinPoint.proceed()
            );
        } catch (Exception e) {
            log.error("Lock execution failed. key={}, type={}, method={}", key, distributedLock.type(), method.getName(), e);
            throw e;
        }
    }
}
