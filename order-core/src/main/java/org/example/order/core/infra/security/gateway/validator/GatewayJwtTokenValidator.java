package org.example.order.core.infra.security.gateway.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Gateway용 JWT 검증 컴포넌트
 * - 기본 토큰 유효성
 * - 추가 Claim 검증 (예: IP, Device, Scope)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayJwtTokenValidator {

    private final JwtTokenManager jwtTokenManager;

    /**
     * 기본 JWT 유효성 체크
     */
    public boolean isValid(String token) {
        return jwtTokenManager.validateToken(token);
    }

    /**
     * 추가 Claim 유효성 체크
     */
    public boolean validateClaims(String token, ServerWebExchange exchange) {
        try {
            String requestIp = extractIp(exchange);
            var claims = jwtTokenManager.getClaims(token);

            String tokenIp = (String) claims.get("ip");
            if (tokenIp != null && !tokenIp.equals(requestIp)) {
                log.warn("[Gateway] IP mismatch. tokenIp={}, requestIp={}", tokenIp, requestIp);
                return false;
            }

            // 추가적으로 디바이스, Scope 체크 가능 (필요 시)
            return true;
        } catch (Exception e) {
            log.error("[Gateway] Failed to validate claims", e);
            return false;
        }
    }

    /**
     * 요청에서 IP 추출
     */
    private String extractIp(ServerWebExchange exchange) {
        return exchange.getRequest()
                .getHeaders()
                .getFirst("X-Forwarded-For");
    }
}
