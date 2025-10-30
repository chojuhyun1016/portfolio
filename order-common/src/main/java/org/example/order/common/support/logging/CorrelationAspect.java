package org.example.order.common.support.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CorrelationAspect
 * - SpEL로 추출한 비즈니스 키를 MDC에 주입.
 * - overrideTraceId=true 이고 실제로 trace를 덮어썼으면 finally에서 복원하지 않음.
 */
@Aspect
public class CorrelationAspect {

    private static final String TRACE_ID = "traceId";

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();
    private final ConcurrentMap<String, Expression> exprCache = new ConcurrentHashMap<>();

    @Around("@annotation(correlate)")
    public Object around(ProceedingJoinPoint pjp, Correlate correlate) throws Throwable {

        Method sigMethod = ((MethodSignature) pjp.getSignature()).getMethod();
        Method method = AopUtils.getMostSpecificMethod(sigMethod, pjp.getTarget().getClass());

        EvaluationContext ctx = new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), pnd);

        // 1) paths 우선
        String extracted = tryPaths(correlate.paths(), ctx);

        // 2) 실패 시 key 보조
        if (isBlank(extracted) && !isBlank(correlate.key())) {
            extracted = evalSpel(correlate.key(), ctx);
        }

        // 3) MDC 주입
        String prevTrace = MDC.get(TRACE_ID);
        String mdcKey = correlate.mdcKey();
        String prevExtra = (!isBlank(mdcKey) ? MDC.get(mdcKey) : null);

        boolean changedTrace = false;

        if (!isBlank(extracted)) {
            if (!isBlank(mdcKey)) {
                MDC.put(mdcKey, extracted);
            }

            if (correlate.overrideTraceId()) {
                if (!extracted.equals(prevTrace)) {
                    changedTrace = true;
                }

                MDC.put(TRACE_ID, extracted);
            }
        }

        try {
            return pjp.proceed();
        } finally {
            if (!isBlank(mdcKey)) {
                if (prevExtra != null) {
                    MDC.put(mdcKey, prevExtra);
                } else {
                    MDC.remove(mdcKey);
                }
            }

            if (correlate.overrideTraceId()) {
                if (!changedTrace) {
                    if (prevTrace != null) {
                        MDC.put(TRACE_ID, prevTrace);
                    } else {
                        MDC.remove(TRACE_ID);
                    }
                }
            }
        }
    }

    private String tryPaths(String[] paths, EvaluationContext ctx) {
        if (paths == null || paths.length == 0) {
            return null;
        }

        for (String p : paths) {
            String v = evalSpel(p, ctx);

            if (!isBlank(v)) {
                return v;
            }
        }

        return null;
    }

    private String evalSpel(String exprText, EvaluationContext ctx) {
        try {
            if (isBlank(exprText)) {
                return null;
            }

            Expression expr = exprCache.computeIfAbsent(exprText, k -> parser.parseExpression(k));
            Object v = expr.getValue(ctx);

            return v == null ? null : String.valueOf(v);
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
