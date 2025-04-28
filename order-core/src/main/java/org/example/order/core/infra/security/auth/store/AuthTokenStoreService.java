package org.example.order.core.infra.security.auth.store;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.redis.repository.RedisRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 액세스 토큰, JTI 저장 및 블랙리스트 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class AuthTokenStoreService {

    private static final String JTI_PREFIX = "jti:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final RedisRepository redisRepository;

    /**
     * JTI 저장
     */
    public void storeJti(String jti, long ttlSeconds) {
        redisRepository.set(JTI_PREFIX + jti, "valid", ttlSeconds);
    }

    /**
     * JTI 유효성 검증
     */
    public boolean isJtiValid(String jti) {
        return redisRepository.get(JTI_PREFIX + jti) != null;
    }

    /**
     * AccessToken 블랙리스트 등록
     */
    public void blacklistAccessToken(String token, long ttlSeconds) {
        redisRepository.set(BLACKLIST_PREFIX + token, "true", ttlSeconds);
    }

    /**
     * AccessToken 블랙리스트 여부 확인
     */
    public boolean isBlacklisted(String token) {
        return "true".equals(redisRepository.get(BLACKLIST_PREFIX + token));
    }
}
