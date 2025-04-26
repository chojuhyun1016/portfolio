package org.example.order.core.infra.security.jwt.util;

import org.springframework.web.server.ServerWebExchange;

public class JwtWebHeaderResolver {

    public static String resolveToken(ServerWebExchange exchange) {
        String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");

        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}
