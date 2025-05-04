package org.example.order.core.infra.security.jwt.util;

import jakarta.servlet.http.HttpServletRequest;
import org.example.order.core.infra.security.jwt.constant.JwtHeaderConstants;
import org.springframework.web.server.ServerWebExchange;

/**
 * JWT 헤더 추출 유틸리티 클래스.
 * <p>
 * HTTP 요청 또는 ServerWebExchange에서 Authorization 헤더를 읽고,
 * Bearer 토큰을 추출하는 기능을 제공합니다.
 */
public final class JwtHeaderResolver {

    // 인스턴스화 방지
    private JwtHeaderResolver() {
    }

    /**
     * ServerWebExchange에서 Bearer 토큰을 추출합니다.
     *
     * @param exchange WebFlux 환경의 요청 객체
     * @return Bearer 토큰 문자열 (없으면 null)
     */
    public static String resolveToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest()
                .getHeaders()
                .getFirst(JwtHeaderConstants.AUTHORIZATION);

        return extractToken(bearer);
    }

    /**
     * HttpServletRequest에서 Bearer 토큰을 추출합니다.
     *
     * @param request Servlet 환경의 요청 객체
     * @return Bearer 토큰 문자열 (없으면 null)
     */
    public static String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(JwtHeaderConstants.AUTHORIZATION);

        return extractToken(bearer);
    }

    /**
     * Bearer 접두사가 포함된 전체 Authorization 헤더에서 순수 토큰만 추출합니다.
     *
     * @param bearer Authorization 헤더 값 (ex: "Bearer eyJhbGciOiJIUzI1Ni...")
     * @return Bearer 접두사를 제거한 토큰 문자열 (없으면 null)
     */
    private static String extractToken(String bearer) {
        return (bearer != null && bearer.startsWith(JwtHeaderConstants.BEARER_PREFIX))
                ? bearer.substring(JwtHeaderConstants.BEARER_PREFIX.length())
                : null;
    }
}
