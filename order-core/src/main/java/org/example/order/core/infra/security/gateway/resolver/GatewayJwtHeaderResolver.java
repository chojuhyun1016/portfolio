package org.example.order.core.infra.security.gateway.resolver;

import org.springframework.web.server.ServerWebExchange;

/**
 * JWT 토큰 추출 유틸 (Authorization 헤더 → 토큰 추출)
 */
public class GatewayJwtHeaderResolver {

    protected static final String BEARER_PREFIX = "Bearer ";

    public static String resolveToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (bearer != null && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
