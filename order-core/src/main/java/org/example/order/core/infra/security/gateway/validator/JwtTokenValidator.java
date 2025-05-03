package org.example.order.core.infra.security.gateway.validator;

import org.springframework.web.server.ServerWebExchange;

/**
 * JWT 토큰 검증 인터페이스
 */
public interface JwtTokenValidator {

    /**
     * 기본 유효성 검증 (서명, 만료 등)
     */
    boolean isValid(String token);

    /**
     * 추가 Claims 검증 (IP, 디바이스 등)
     */
    boolean validateClaims(String token, ServerWebExchange exchange, String path);
}
