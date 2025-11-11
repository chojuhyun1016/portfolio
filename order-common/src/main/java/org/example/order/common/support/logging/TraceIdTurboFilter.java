package org.example.order.common.support.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.UUID;

/**
 * TraceIdTurboFilter
 * ------------------------------------------------------------------------
 * 목적
 * - 모든 로깅 이벤트에 대해 MDC["traceId"]가 비어있으면 즉시 UUID를 생성/주입한다.
 * - 배치/콘솔/프레임워크 초기화 로그 등 AOP 밖 구간까지 포괄적으로 보장한다.
 * - 이후 애플리케이션 레이어(@Correlate(overrideTraceId=true) 등)가
 * 도메인 키(orderId 등)를 추출하면 해당 값으로 자연스럽게 덮어쓴다.
 * <p>
 * 권장 패턴
 * - 로그 패턴에 [%X{traceId:-NA}] 가 포함되어 있어야 최종 출력된다.
 * - order-worker: 리스너/서비스에서 키값을 traceId로 덮어씀(overrideTraceId=true).
 * - order-batch : 키값 덮어쓰기 없이 UUID 유지(overrideTraceId=false 또는 미사용).
 */
public class TraceIdTurboFilter extends TurboFilter {

    private static final String TRACE_ID = "traceId";

    @Override
    public FilterReply decide(Marker marker,
                              Logger logger,
                              Level level,
                              String format,
                              Object[] params,
                              Throwable t) {
        try {
            ensureTraceId();
        } catch (Exception ignore) {
            // MDC 사용이 불가한 극초기 상황에서도 절대 로깅을 방해하지 않는다.
        }

        return FilterReply.NEUTRAL;
    }

    private static void ensureTraceId() {
        String cur = MDC.get(TRACE_ID);

        if (cur == null || cur.isBlank()) {
            MDC.put(TRACE_ID, UUID.randomUUID().toString());
        }
    }
}
