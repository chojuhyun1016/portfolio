package org.example.order.core.infra.security.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.gateway.security.JwtTokenValidator;
import org.example.order.core.infra.security.jwt.util.JwtWebHeaderResolver;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAuthenticationFilter implements GatewayFilter {

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = JwtWebHeaderResolver.resolveToken(exchange);

        if (token != null && jwtTokenValidator.validate(token)) {
            if (!jwtTokenValidator.validateClaims(token, exchange)) {
                log.warn("[Gateway] Claim validation failed");
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);

                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        }

        log.warn("[Gateway] JWT missing or invalid");
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }
}
