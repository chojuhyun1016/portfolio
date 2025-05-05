package org.example.order.core.infra.security.oauth2.core.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.redis.repository.Oauth2TokenRepository;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.springframework.stereotype.Component;

/**
 * Oauth2 토큰 검증 책임 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Oauth2TokenValidator {

    private final Oauth2TokenRepository tokenRepository;
    private final TokenProvider tokenProvider;

    public boolean isAccessTokenValid(String token) {
        if (!tokenProvider.validateToken(token)) {
            log.warn("JWT 서명 검증 실패");

            return false;
        }
        if (tokenRepository.isBlacklisted(token)) {
            log.warn("블랙리스트 토큰 검출");

            return false;
        }
        return true;
    }

    public boolean isRefreshTokenValid(String userId, String refreshToken) {
        return tokenRepository.validateRefreshToken(userId, refreshToken);
    }
}
