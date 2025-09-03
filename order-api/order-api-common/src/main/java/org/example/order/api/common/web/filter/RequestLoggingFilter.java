package org.example.order.api.common.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.common.infra.ApiInfraProperties;

import java.io.IOException;

/**
 * 간단한 요청 라인 로깅 필터.
 * 필요 시 본문 로깅/샘플링/마스킹으로 확장 가능.
 */
@Slf4j
public class RequestLoggingFilter implements Filter {

    private final ApiInfraProperties props;

    public RequestLoggingFilter(ApiInfraProperties props) {
        this.props = props;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;

        log.info("REQ {} {}", r.getMethod(), r.getRequestURI());

        chain.doFilter(req, res);
    }
}
