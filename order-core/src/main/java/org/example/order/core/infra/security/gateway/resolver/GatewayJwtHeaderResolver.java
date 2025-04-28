package org.example.order.core.infra.security.gateway.resolver;

import org.springframework.web.server.ServerWebExchange;

/**
 * Gateway 요청에서 Authorization 헤더로부터
 * JWT 토큰을 추출하는 유틸리티
 */
public class GatewayJwtHeaderResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    public static String resolveToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (bearer != null && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
