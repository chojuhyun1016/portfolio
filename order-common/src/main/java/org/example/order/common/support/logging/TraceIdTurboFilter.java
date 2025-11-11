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
 * <p>
 * 주의
 * - 패턴에 [%X{traceId:-NA}] 가 포함되어 있어야 최종 출력된다.
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
            String cur = MDC.get(TRACE_ID);

            if (cur == null || cur.isBlank()) {
                MDC.put(TRACE_ID, UUID.randomUUID().toString());
            }
        } catch (Exception ignore) {
            // MDC 사용이 불가한 극초기 상황에서도 절대 로깅을 방해하지 않는다.
        }

        return FilterReply.NEUTRAL;
    }
}
