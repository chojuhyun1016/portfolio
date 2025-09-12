package org.example.order.common.support.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CorrelationAspect
 * ------------------------------------------------------------------------
 * 목적
 * - 메서드 파라미터에서 SpEL로 추출한 "도메인 키"를 MDC에 주입하여 로그 상관관계(trace)를 강화한다.
 * - 선택적으로 traceId를 도메인 키로 덮어써서 전 구간 추적 키를 비즈니스 키로 통일할 수 있다.
 * <p>
 * 동작
 * - @Correlate(key="...")가 붙은 메서드 호출 시:
 * 1) SpEL로 값(extracted) 계산(null/blank면 무시)
 * 2) mdcKey가 있으면 MDC[mdcKey] = extracted
 * 3) overrideTraceId=true면 MDC["traceId"] = extracted
 * 4) 실행 후 기존 MDC 값 복원(try/finally)
 * <p>
 * 주의
 * - MDC는 ThreadLocal 기반: @Async/별도 스레드풀/콜백 경계에서는 TaskDecorator 등으로 전파 필요.
 * - 본 빈은 오토컨피그(LoggingAutoConfiguration)에서 등록된다(@Component 스캔 의존 X).
 * <p>
 * (추가)
 * - 표현식 캐시 도입(메서드 기준).
 * - MethodBasedEvaluationContext 사용으로 #p0/#a0 및 파라미터명(-parameters 유무) 모두 지원.
 */
@Aspect
public class CorrelationAspect {

    private static final String TRACE_ID = "traceId";

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();
    private final ConcurrentMap<Method, Expression> exprCache = new ConcurrentHashMap<>();

    @Around("@annotation(correlate)")
    public Object around(ProceedingJoinPoint pjp, Correlate correlate) throws Throwable {

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        // 1) SpEL Expression 캐시
        Expression expr = exprCache.computeIfAbsent(method, m -> parser.parseExpression(correlate.key()));

        // 2) 표준 메서드 기반 EvaluationContext (파라미터명 / #p0, #a0 모두 지원)
        EvaluationContext ctx = new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), pnd);

        // 3) SpEL 평가
        String extracted = null;
        try {
            Object v = expr.getValue(ctx);
            if (v != null) {
                extracted = String.valueOf(v);
            }
        } catch (Exception ignore) {
            // 필요 시 디버깅 로그 추가 가능
            // log.debug("Correlate SpEL eval failed: {}", correlate.key(), ignore);
        }

        // 4) 기존값 백업
        String prevTrace = MDC.get(TRACE_ID);
        String mdcKey = correlate.mdcKey();
        String prevExtra = (mdcKey != null && !mdcKey.isBlank()) ? MDC.get(mdcKey) : null;

        // 5) 주입
        if (extracted != null && !extracted.isBlank()) {
            if (mdcKey != null && !mdcKey.isBlank()) {
                MDC.put(mdcKey, extracted);
            }
            if (correlate.overrideTraceId()) {
                MDC.put(TRACE_ID, extracted);
            }
        }

        try {
            return pjp.proceed();
        } finally {
            // 6) 복원
            if (mdcKey != null && !mdcKey.isBlank()) {
                if (prevExtra != null) MDC.put(mdcKey, prevExtra);
                else MDC.remove(mdcKey);
            }
            if (correlate.overrideTraceId()) {
                if (prevTrace != null) MDC.put(TRACE_ID, prevTrace);
                else MDC.remove(TRACE_ID);
            }
        }
    }
}
