package org.example.order.core.infra.security.service;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.dto.TokenResponseDto;
import org.example.order.core.infra.security.jwt.JwtTokenProvider;
import org.example.order.core.infra.security.redis.RefreshTokenRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    public TokenResponseDto issueTokens(String userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenRepository.save(userId, refreshToken, jwtTokenProvider.getRefreshValiditySeconds());

        return new TokenResponseDto(accessToken, refreshToken);
    }

    public TokenResponseDto reissue(String userId, String oldRefreshToken) {
        String storedToken = refreshTokenRepository.get(userId);
        if (storedToken == null || !storedToken.equals(oldRefreshToken)) {
            throw new IllegalStateException("Refresh token mismatch");
        }

        return issueTokens(userId);
    }

    public void logout(String userId, String accessToken) {
        refreshTokenRepository.delete(userId);

        long ttl = jwtTokenProvider.getTokenRemainingSeconds(accessToken);
        if (ttl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "true", ttl, TimeUnit.SECONDS);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.toString().equals(redisTemplate.opsForValue().get(BLACKLIST_PREFIX + accessToken));
    }
}
