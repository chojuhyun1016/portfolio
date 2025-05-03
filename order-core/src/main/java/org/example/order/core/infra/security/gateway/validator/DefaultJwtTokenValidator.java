package org.example.order.core.infra.security.gateway.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * 기본 JWT 검증 구현:
 * - 기본 토큰 유효성 + Claims 검증 (IP, Scope 등)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultJwtTokenValidator implements JwtTokenValidator {

    protected static final String LOG_PREFIX = "[JwtValidator]";
    protected static final String ADMIN_SCOPE = "admin";
    protected static final String ADMIN_PATH_PREFIX = "/api/v1/admin";

    private final JwtTokenManager jwtTokenManager;

    @Override
    public boolean isValid(String token) {
        return jwtTokenManager.validateToken(token);
    }

    @Override
    public boolean validateClaims(String token, ServerWebExchange exchange, String path) {
        try {
            String requestIp = extractIp(exchange);
            var claims = jwtTokenManager.getClaims(token);

            String tokenIp = (String) claims.get("ip");
            if (tokenIp != null && !tokenIp.equals(requestIp)) {
                log.warn("{} IP mismatch. tokenIp={}, requestIp={}", LOG_PREFIX, tokenIp, requestIp);

                return false;
            }

            String scope = (String) claims.get("scope");
            if (!hasRequiredScope(scope, path)) {
                log.warn("{} Scope validation failed. scope={}, path={}", LOG_PREFIX, scope, path);

                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("{} Failed to validate claims", LOG_PREFIX, e);
            return false;
        }
    }

    protected String extractIp(ServerWebExchange exchange) {
        String ipList = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

        if (ipList != null) {
            return ipList.split(",")[0].trim();
        }

        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");

        if (realIp != null) {
            return realIp;
        }

        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : null;
    }

    protected boolean hasRequiredScope(String scope, String path) {
        return !(path.startsWith(ADMIN_PATH_PREFIX) && !ADMIN_SCOPE.equals(scope));
    }
}
