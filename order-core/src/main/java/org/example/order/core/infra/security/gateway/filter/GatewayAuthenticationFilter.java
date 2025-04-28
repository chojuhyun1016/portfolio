package org.example.order.core.infra.security.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.gateway.resolver.GatewayJwtHeaderResolver;
import org.example.order.core.infra.security.gateway.validator.GatewayJwtTokenValidator;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway에서 API 요청을 받을 때
 * JWT 인증 및 Claim 유효성 검증을 수행하는 필터입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAuthenticationFilter implements GatewayFilter {

    private final GatewayJwtTokenValidator jwtTokenValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 요청 헤더에서 JWT 토큰 추출
        String token = GatewayJwtHeaderResolver.resolveToken(exchange);

        // 2. 토큰 존재 및 유효성 검증
        if (token != null && jwtTokenValidator.isValid(token)) {
            // 3. 토큰 Claim 추가 검증 (IP, Device, Scope 등)
            if (!jwtTokenValidator.validateClaims(token, exchange)) {
                log.warn("[Gateway] JWT claim validation failed.");
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // 4. 통과
            return chain.filter(exchange);
        }

        // 5. 토큰 없음 또는 유효성 실패
        log.warn("[Gateway] JWT missing or invalid.");
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
