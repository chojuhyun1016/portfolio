package org.example.order.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CorrelationIdFilter
 * ------------------------------------------------------------------------
 * 목적
 * - 요청 헤더 X-Request-Id를 받아 MDC["requestId"]에 저장.
 * - MDC["traceId"]가 비어있으면 requestId로 브리지(MDC["traceId"]=requestId).
 * - 응답 헤더에도 X-Request-Id를 세팅.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String id = req.getHeader(HEADER);

        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        String prevRequestId = MDC.get(MDC_REQUEST_ID);
        String prevTraceId = MDC.get(MDC_TRACE_ID);

        MDC.put(MDC_REQUEST_ID, id);

        if (prevTraceId == null || prevTraceId.isBlank()) {
            // traceId 없으면 requestId로 브리지
            MDC.put(MDC_TRACE_ID, id);
        }

        try {
            res.setHeader(HEADER, id);
            chain.doFilter(req, res);
        } finally {
            // 기존 값 복원
            if (prevRequestId != null) {
                MDC.put(MDC_REQUEST_ID, prevRequestId);
            } else {
                MDC.remove(MDC_REQUEST_ID);
            }

            if (prevTraceId != null) {
                MDC.put(MDC_TRACE_ID, prevTraceId);
            } else {
                MDC.remove(MDC_TRACE_ID);
            }
        }
    }
}
