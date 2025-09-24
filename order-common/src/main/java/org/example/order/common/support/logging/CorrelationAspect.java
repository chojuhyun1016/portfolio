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
 * ------------------------------------------------------------------------
 * 목적
 * - 메서드 파라미터에서 SpEL로 추출한 "도메인 키"를 MDC에 주입하여 로그 상관관계(trace)를 강화한다.
 * - 선택적으로 traceId를 도메인 키로 덮어써서 전 구간 추적 키를 비즈니스 키로 통일할 수 있다.
 * <p>
 * 동작
 * - @Correlate(paths=..., key=...)가 붙은 메서드 호출 시:
 * 1) paths 배열을 순서대로 평가하여 첫 번째 성공 값을 채택
 * 2) paths 결과가 없으면 key(단일) 평가를 시도
 * 3) 값이 존재할 때만 MDC에 주입 (traceId 및 mdcKey)
 * 4) 실행 후 기존 MDC 값 복원(try/finally)
 * <p>
 * 주의
 * - MDC는 ThreadLocal 기반: @Async/별도 스레드풀/콜백 경계에서는 TaskDecorator 등으로 전파 필요.
 * - 본 빈은 오토컨피그(LoggingAutoConfiguration)에서 등록된다(@Component 스캔 의존 X).
 * <p>
 * (변경 사항)
 * - [중요] 하드코딩 fallback(키/헤더/페이로드 자동 스캔) 제거 → 범용 라이브러리 성격 유지.
 * - 반드시 @Correlate(paths=...)로 “무엇을 추출할지”를 리스너/서비스에서 명시하도록 유도.
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

        // 1) paths 우선 평가
        String extracted = tryPaths(correlate.paths(), ctx);

        // 2) paths 실패 시 key(단일) 보조 평가
        if (isBlank(extracted) && !isBlank(correlate.key())) {
            extracted = evalSpel(correlate.key(), ctx);
        }

        // 3) MDC 주입
        String prevTrace = MDC.get(TRACE_ID);
        String mdcKey = correlate.mdcKey();
        String prevExtra = (!isBlank(mdcKey) ? MDC.get(mdcKey) : null);

        if (!isBlank(extracted)) {
            if (!isBlank(mdcKey)) {
                MDC.put(mdcKey, extracted);
            }

            if (correlate.overrideTraceId()) {
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
                if (prevTrace != null) {
                    MDC.put(TRACE_ID, prevTrace);
                } else {
                    MDC.remove(TRACE_ID);
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
