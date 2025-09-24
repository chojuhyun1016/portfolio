package org.example.order.common.support.logging;

import java.lang.annotation.*;

/**
 * @Correlate ------------------------------------------------------------------------
 * 목적
 * - 메서드 파라미터에서 SpEL로 추출한 "도메인 키"를 MDC에 주입해 로그 상관관계를 개선한다.
 * <p>
 * 속성
 * - paths          : 우선순위 리스트. SpEL 표현식 배열을 순서대로 평가하여 첫 번째로 얻어진 값을 사용.
 * (예: {"#record.key()", "#record.value().id", "#record.headers().lastHeader('orderId')?.value"})
 * - key            : 레거시 호환용 단일 SpEL. paths가 모두 실패했을 때만 평가(선택).
 * - overrideTraceId: true면 추출값으로 MDC["traceId"]를 덮어쓴다(도메인 키 기반의 추적).
 * - mdcKey         : (선택) 보조 MDC 키명. 비워두면 저장 안 함(예: "orderId", "paymentId").
 * <p>
 * 비고
 * - 반드시 paths 또는 key 중 적어도 하나는 유효한 표현식을 제공할 것(실무 권장: paths).
 * - 로그 패턴에 %X{traceId} 또는 %X{orderId} 등을 포함해야 출력된다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Correlate {

    /**
     * 다중 경로 SpEL 우선순위 (첫 번째로 평가 성공하는 값을 사용)
     */
    String[] paths() default {};

    /**
     * 레거시/보조 단일 SpEL (paths가 모두 실패했을 때만 평가)
     */
    String key() default "";

    /**
     * 추출값으로 traceId를 덮어쓸지 여부
     */
    boolean overrideTraceId() default true;

    /**
     * 함께 저장할 MDC 키명 (예: "orderId"). 비워두면 저장 안 함
     */
    String mdcKey() default "";
}
