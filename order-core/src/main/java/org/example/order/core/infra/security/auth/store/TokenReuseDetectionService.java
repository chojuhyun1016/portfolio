package org.example.order.core.infra.security.auth.store;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RefreshToken 재사용 공격 탐지 서비스
 */
@Service
@RequiredArgsConstructor
public class TokenReuseDetectionService {

    private final RefreshTokenStoreService refreshTokenStoreService;

    /**
     * 제시된 토큰이 저장된 토큰과 불일치하는 경우 재사용 공격 탐지
     */
    public boolean isReuseDetected(String userId, String presentedToken) {
        String storedToken = refreshTokenStoreService.getRefreshToken(userId);
        return storedToken == null || !storedToken.equals(presentedToken);
    }
}
