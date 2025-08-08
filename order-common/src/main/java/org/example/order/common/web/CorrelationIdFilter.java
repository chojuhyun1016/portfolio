package org.example.order.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * X-Request-Id가 없으면 생성하여 응답에도 반영하고 MDC에 저장.
 * - Bean 등록/순서는 각 서버(Web/Security Config)에서 결정
 * - 설정(yml) 읽지 않음
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader(HEADER);

        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, id);

        try {
            res.setHeader(HEADER, id);
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}