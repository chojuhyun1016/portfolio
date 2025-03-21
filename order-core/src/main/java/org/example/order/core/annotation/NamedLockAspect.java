package org.example.order.core.annotation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class NamedLockAspect {

    @PersistenceContext
    private EntityManager em;
    private final AopForTransaction aopForTransaction;

    @Around("@annotation(namedLock)")
    public Object namedLockAspect(ProceedingJoinPoint joinPoint, NamedLock namedLock) throws Throwable {
        Long lock = 0L;
        try {

            // SpEL로 메서드 파라미터 값 가져오기
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();

            // SpEL 파서 초기화
            ExpressionParser parser = new SpelExpressionParser();
            EvaluationContext context = new StandardEvaluationContext();

            // 메서드 파라미터 바인딩
            Object[] args = joinPoint.getArgs();
            String[] parameterNames = signature.getParameterNames();
            for (int i = 0; i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }

            // SpEL 파싱
            String lockName = parser.parseExpression(namedLock.lockName()).getValue(context, String.class);

            log.info("NamedLockAspect LockName : {} ", lockName);

            lock = (Long) em.createNativeQuery("SELECT GET_LOCK(:lockName, 3)")
                    .setParameter("lockName", lockName)
                    .getSingleResult();

            // exception
            if (lock == null || lock == 0L) {
                log.error("NamedLock get fail : lockName={} ", lockName);

                throw new IllegalStateException("NamedLock get fail : lockName=" + lockName);
            }

            // success
            else {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        em.createNativeQuery("SELECT RELEASE_LOCK(:lockName)")
                                .setParameter("lockName", lockName)
                                .getSingleResult();
                    }
                });
            }
        }
        catch (Exception e) {
            log.error("namedLockAspect fail : e.getMessage = {} ", e.getMessage());
        }

        // 실제 메소드 호출 진행
        Object[] newArgs = Arrays.copyOf(joinPoint.getArgs(), joinPoint.getArgs().length);
        newArgs[newArgs.length - 1] = lock != null && lock == 1;

        return aopForTransaction.proceed(joinPoint, newArgs);
    }
}
