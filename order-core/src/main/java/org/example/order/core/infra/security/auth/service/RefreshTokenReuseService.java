package org.example.order.core.infra.security.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenReuseService {

    private final AuthTokenStoreService authTokenStoreService;

    public boolean isReuseDetected(String userId, String presentedToken) {
        String storedToken = authTokenStoreService.getRefreshToken(userId);
        return storedToken == null || !storedToken.equals(presentedToken);
    }
}
