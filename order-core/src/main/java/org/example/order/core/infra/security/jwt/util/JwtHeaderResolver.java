package org.example.order.core.infra.security.jwt.util;

import jakarta.servlet.http.HttpServletRequest;
import org.example.order.core.infra.security.jwt.constant.JwtTokenConstants;
import org.springframework.web.server.ServerWebExchange;

/**
 * JWT 헤더 추출 유틸
 */
public final class JwtHeaderResolver {

    private JwtHeaderResolver() {
    }

    public static String resolveToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest().getHeaders().getFirst(JwtTokenConstants.HEADER_AUTHORIZATION);
        return extractToken(bearer);
    }

    public static String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(JwtTokenConstants.HEADER_AUTHORIZATION);
        return extractToken(bearer);
    }

    private static String extractToken(String bearer) {
        return (bearer != null && bearer.startsWith(JwtTokenConstants.BEARER_PREFIX))
                ? bearer.substring(JwtTokenConstants.BEARER_PREFIX.length())
                : null;
    }
}
