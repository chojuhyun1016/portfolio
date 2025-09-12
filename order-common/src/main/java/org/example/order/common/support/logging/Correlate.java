package org.example.order.common.support.logging;

import java.lang.annotation.*;

/**
 * @Correlate ------------------------------------------------------------------------
 * 목적
 * - 메서드 파라미터에서 SpEL로 추출한 "도메인 키"를 MDC에 주입해 로그 상관관계를 개선한다.
 * <p>
 * 속성
 * - key             : SpEL 표현식. 메서드 파라미터로부터 추적키를 추출(예: "#command.orderId").
 * - overrideTraceId : true면 추출값으로 MDC["traceId"]를 덮어쓴다(도메인 키 기반의 추적).
 * - mdcKey          : (선택) 보조 MDC 키명. 비워두면 저장 안 함(예: "orderId", "paymentId").
 * <p>
 * 예시
 * - @Correlate(key = "#command.orderId", mdcKey = "orderId", overrideTraceId = true)
 * - @Correlate(key = "#payment.id", overrideTraceId = true)                // traceId만 변경
 * - @Correlate(key = "#user.id", mdcKey = "userId", overrideTraceId = false) // 보조키만 저장
 * <p>
 * 비고
 * - 로그 패턴에 %X{traceId} 또는 %X{orderId} 등을 포함해야 출력된다.
 * - MDC는 ThreadLocal이므로 비동기/콜백 경계에서는 별도 전파 설정(TaskDecorator 등)이 필요하다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Correlate {

    /**
     * SpEL로 파라미터에서 도메인 키 추출 (예: "#command.orderId", "#message.id")
     */
    String key();

    /**
     * 추출값으로 traceId를 덮어쓸지 여부
     */
    boolean overrideTraceId() default true;

    /**
     * 함께 저장할 MDC 키명 (예: "orderId"). 비워두면 저장 안 함
     */
    String mdcKey() default "";
}
