package org.example.order.core.infra.security.token;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.service.TokenStoreService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenReuseDetector {

    private final TokenStoreService tokenStoreService;

    public boolean isReuseDetected(String userId, String presentedToken) {
        String storedToken = tokenStoreService.getRefreshToken(userId);
        return storedToken == null || !storedToken.equals(presentedToken);
    }
}
