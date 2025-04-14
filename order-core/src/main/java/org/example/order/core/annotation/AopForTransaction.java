package org.example.order.core.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AopForTransaction {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object proceedT(final ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }

    @Transactional
    public Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
