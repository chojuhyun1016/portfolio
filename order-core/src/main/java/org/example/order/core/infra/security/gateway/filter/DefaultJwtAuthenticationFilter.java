package org.example.order.core.infra.security.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.gateway.validator.JwtTokenValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 기본 JWT 인증 필터 (Gateway)
 * - Authorization 헤더의 Bearer 토큰을 추출하여 유효성/클레임 검증
 * - jwt.enabled=true 이고 JwtTokenValidator 빈이 있을 때만 활성화
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "true")
@ConditionalOnBean(JwtTokenValidator.class)
public class DefaultJwtAuthenticationFilter implements WebFilter {

    private static final String LOG_PREFIX = "[JwtAuthFilter]";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator validator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            // 1) 토큰 유효성
            if (!validator.isValid(token)) {
                log.warn("{} invalid token", LOG_PREFIX);
                // 여기서 바로 끊고 싶다면 401 응답 반환 로직 추가 가능
                return chain.filter(exchange); // 현재는 무시하고 계속 진행
            }

            // 2) 추가 클레임 검증 (IP, scope 등)
            String path = exchange.getRequest().getURI().getPath();
            if (!validator.validateClaims(token, exchange, path)) {
                log.warn("{} claims validation failed, path={}", LOG_PREFIX, path);
                return chain.filter(exchange);
            }
        }

        return chain.filter(exchange);
    }
}
