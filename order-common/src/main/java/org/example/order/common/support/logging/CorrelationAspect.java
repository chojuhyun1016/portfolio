package org.example.order.common.support.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

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
 */
@Aspect
public class CorrelationAspect {

    private static final String TRACE_ID = "traceId";

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer pnd = new DefaultParameterNameDiscoverer();

    @Around("@annotation(correlate)")
    public Object around(ProceedingJoinPoint pjp, Correlate correlate) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        // SpEL 평가 컨텍스트(파라미터 바인딩)
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        String[] names = pnd.getParameterNames(method);
        Object[] args = pjp.getArgs();

        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                ctx.setVariable(names[i], args[i]);
            }
        }

        // 1) SpEL 평가
        String extracted = null;

        try {
            Object v = parser.parseExpression(correlate.key()).getValue(ctx);

            if (v != null) {
                extracted = String.valueOf(v);
            }
        } catch (Exception ignore) {
            /* 무시 */
        }

        // 2) 기존값 백업
        String prevTrace = MDC.get(TRACE_ID);
        String mdcKey = correlate.mdcKey();
        String prevExtra = (mdcKey != null && !mdcKey.isBlank()) ? MDC.get(mdcKey) : null;

        // 3) 주입
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
            // 4) 복원
            if (mdcKey != null && !mdcKey.isBlank()) {
                if (prevExtra != null) {
                    MDC.put(mdcKey, prevExtra);
                } else {
                    MDC.remove(mdcKey);
                }
            }
            if (correlate.overrideTraceId()) {
                if (prevTrace != null) {
                    MDC.put(TRACE_ID, prevTrace);
                } else {
                    MDC.remove(TRACE_ID);
                }
            }
        }
    }
}
