package org.example.order.core.infra.security.oauth2.core.provider;

import lombok.RequiredArgsConstructor;
import org.example.order.core.application.security.vo.Oauth2TokenIssueRequest;
import org.example.order.core.infra.redis.repository.Oauth2TokenRepository;
import org.example.order.core.infra.security.oauth2.core.contract.Oauth2TokenProvider;
import org.example.order.core.infra.security.oauth2.core.issuer.Oauth2TokenIssuer;
import org.example.order.core.infra.security.oauth2.core.validator.Oauth2TokenValidator;
import org.example.order.core.application.security.response.Oauth2AccessToken;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 표준 Oauth2 토큰 Provider (발급/검증 책임 분리)
 */
@Service
@RequiredArgsConstructor
public class DefaultOauth2TokenProvider implements Oauth2TokenProvider {

    private final Oauth2TokenIssuer tokenIssuer;
    private final Oauth2TokenValidator tokenValidator;
    private final Oauth2TokenRepository tokenRepository;

    @Override
    public Oauth2AccessToken createAccessToken(Oauth2TokenIssueRequest request) {
        return tokenIssuer.issueToken(
                request,
                request.getScopes(),
                request.getDeviceId(),
                request.getIpAddress()
        );
    }

    @Override
    public Oauth2AccessToken refreshAccessToken(String userId, String oldRefreshToken) {
        if (!tokenValidator.isRefreshTokenValid(userId, oldRefreshToken)) {
            throw new IllegalArgumentException("Invalid RefreshToken");
        }

        tokenRepository.deleteRefreshToken(userId);

        Oauth2TokenIssueRequest req = Oauth2TokenIssueRequest.builder()
                .userId(userId)
                .roles(List.of())
                .scopes(List.of())
                .deviceId(null)
                .ipAddress(null)
                .build();

        return tokenIssuer.issueToken(
                req,
                req.getScopes(),
                req.getDeviceId(),
                req.getIpAddress()
        );
    }

    @Override
    public boolean validateAccessToken(String token) {
        return tokenValidator.isAccessTokenValid(token);
    }

    @Override
    public boolean validateRefreshToken(String userId, String refreshToken) {
        return tokenValidator.isRefreshTokenValid(userId, refreshToken);
    }
}
