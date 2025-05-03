package org.example.order.core.infra.security.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.gateway.matcher.WhiteListMatcher;
import org.example.order.core.infra.security.gateway.resolver.GatewayJwtHeaderResolver;
import org.example.order.core.infra.security.gateway.validator.JwtTokenValidator;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 추상 JWT 인증 Global Filter:
 * - 화이트리스트 매칭 + JWT 인증 처리
 * - 커스텀 인증 필터 구현 시 이 클래스를 상속
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractJwtAuthenticationFilter implements GlobalFilter {

    protected static final String LOG_PREFIX = "[Auth Filter]";

    private final JwtTokenValidator jwtTokenValidator;
    private final WhiteListMatcher whiteListMatcher;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (whiteListMatcher.isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = GatewayJwtHeaderResolver.resolveToken(exchange);

        if (token == null) {
            log.warn("{} Missing JWT token. path={}", LOG_PREFIX, path);

            return unauthorized(exchange);
        }

        if (!jwtTokenValidator.isValid(token)) {
            log.warn("{} Invalid JWT token. path={}", LOG_PREFIX, path);

            return unauthorized(exchange);
        }

        if (!jwtTokenValidator.validateClaims(token, exchange, path)) {
            log.warn("{} JWT claims validation failed. path={}", LOG_PREFIX, path);

            return forbidden(exchange);
        }

        return chain.filter(exchange);
    }

    protected Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    protected Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);

        return exchange.getResponse().setComplete();
    }
}
