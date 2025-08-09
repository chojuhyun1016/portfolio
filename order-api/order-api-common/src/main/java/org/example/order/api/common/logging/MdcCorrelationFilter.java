package org.example.order.api.common.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.order.api.common.infra.ApiInfraProperties;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 식별자(requestId)를 MDC에 주입하고 응답 헤더로도 전달하는 필터.
 * - 헤더가 없으면 UUID 생성
 * - 로그 패턴에서 %X{requestId}로 사용 가능
 */
public class MdcCorrelationFilter implements Filter {

    private final ApiInfraProperties props;

    public MdcCorrelationFilter(ApiInfraProperties props) {
        this.props = props;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        String inHeader = props.getLogging().getIncomingHeader();
        String outHeader = props.getLogging().getResponseHeader();

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String reqId = request.getHeader(inHeader);
        if (reqId == null || reqId.isBlank()) {
            reqId = UUID.randomUUID().toString();
        }

        MDC.put("requestId", reqId);
        response.setHeader(outHeader, reqId);

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("requestId");
        }
    }
}
