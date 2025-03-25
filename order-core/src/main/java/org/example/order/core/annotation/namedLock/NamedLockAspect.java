package org.example.order.core.annotation.namedLock;

import org.example.order.common.code.CommonExceptionCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.order.common.exception.CommonException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class NamedLockAspect {

    @PersistenceContext
    private EntityManager em;
    private final AopForTransaction aopForTransaction;

    // REQUIRED_NEW
    @Around("@annotation(namedLockT)")
    public Object namedLockTAspect(ProceedingJoinPoint joinPoint, NamedLockT namedLockT) throws Throwable {
        String lockName = "";

        try {
            lockName = this.getLock(joinPoint, namedLockT.lockName());

            // 실제 메소드 호출 진행
            return aopForTransaction.proceedT(joinPoint);
        }
        catch (Exception e) {
            log.error("namedLockAspect fail : e.getMessage = {} ", e.getMessage());

            throw new CommonException(CommonExceptionCode.DATABASE_LOCK_ERROR);
        }
        finally {
            log.info("release lock : {} ", lockName);

            em.createNativeQuery("SELECT RELEASE_LOCK(?1)")
                    .setParameter(1, lockName)
                    .getSingleResult();
        }
    }

    // REQUIRED
    @Around("@annotation(namedLock)")
    public Object namedLockAspect(ProceedingJoinPoint joinPoint, NamedLock namedLock) throws Throwable {
        String lockName = "";

        try {
            lockName = this.getLock(joinPoint, namedLock.lockName());

            // 실제 메소드 호출 진행
            return aopForTransaction.proceed(joinPoint);
        }
        catch (Exception e) {
            log.error("namedLockAspect fail : e.getMessage = {} ", e.getMessage());

            throw new CommonException(CommonExceptionCode.DATABASE_LOCK_ERROR);
        }
        finally {
            log.info("release lock : {} ", lockName);

            em.createNativeQuery("SELECT RELEASE_LOCK(?1)")
                    .setParameter(1, lockName)
                    .getSingleResult();
        }
    }

    private String getLock(ProceedingJoinPoint joinPoint, String namedLockName) {
        Long lock = 0L;
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
        String lockName = parser.parseExpression(namedLockName).getValue(context, String.class);

        log.info("NamedLockAspect LockName : {} ", lockName);

        lock = (Long) em.createNativeQuery("SELECT GET_LOCK(?1, 50)")
                .setParameter(1, lockName)
                .getSingleResult();

        // exception
        if (lock == null || lock == 0L) {
            log.error("NamedLock get fail : lockName={} ", lockName);

            throw new IllegalStateException("NamedLock get fail : lockName=" + lockName);
        }

        return lockName;
    }
}
