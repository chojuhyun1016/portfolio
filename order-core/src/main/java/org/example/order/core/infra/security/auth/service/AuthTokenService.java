package org.example.order.core.infra.security.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.auth.dto.AuthTokenResponse;
import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
import org.example.order.core.infra.security.redis.RefreshTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtTokenManager jwtTokenManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    public AuthTokenResponse issueTokens(String userId, String device, String aud, List<String> scopes, String ip) {
        String jti = UUID.randomUUID().toString();

        String accessToken = jwtTokenManager.createAccessToken(
                userId,
                List.of("ROLE_USER"),
                jti,
                device,
                aud,
                scopes,
                ip
        );

        String refreshToken = jwtTokenManager.createRefreshToken(userId, jti);

        // Redis 저장 (TTL = refreshToken 만료 시간 기준)
        long refreshTtlSeconds = jwtTokenManager.getRemainingSeconds(refreshToken);
        refreshTokenRepository.save(userId, refreshToken, refreshTtlSeconds);

        return new AuthTokenResponse(accessToken, refreshToken);
    }

    public AuthTokenResponse reissue(String userId, String oldRefreshToken, String device, String aud, List<String> scopes, String ip) {
        String storedToken = refreshTokenRepository.get(userId);
        if (storedToken == null || !storedToken.equals(oldRefreshToken)) {
            throw new IllegalStateException("Refresh token mismatch");
        }

        return issueTokens(userId, device, aud, scopes, ip);
    }

    public void logout(String userId, String accessToken) {
        refreshTokenRepository.delete(userId);

        long ttl = jwtTokenManager.getRemainingSeconds(accessToken);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "true", ttl, TimeUnit.SECONDS);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.toString().equals(redisTemplate.opsForValue().get(BLACKLIST_PREFIX + accessToken));
    }
}
