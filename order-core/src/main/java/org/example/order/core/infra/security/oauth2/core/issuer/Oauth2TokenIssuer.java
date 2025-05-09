package org.example.order.core.infra.security.oauth2.core.issuer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.security.entity.Oauth2RefreshTokenRedisEntity;
import org.example.order.core.application.security.vo.Oauth2TokenIssueRequest;
import org.example.order.core.infra.redis.repository.Oauth2TokenRepository;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.example.order.core.infra.security.oauth2.config.Oauth2ServerProperties;
import org.example.order.core.application.security.response.Oauth2AccessToken;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Oauth2 토큰 발급 책임 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Oauth2TokenIssuer {

    private final Oauth2ServerProperties oauth2Props;
    private final TokenProvider tokenProvider;
    private final Oauth2TokenRepository tokenRepository;

    public Oauth2AccessToken issueToken(Oauth2TokenIssueRequest request, List<String> scopes, String device, String ip) {
        String userId = request.getUserId();
        String jti = UUID.randomUUID().toString();
        String kid = tokenProvider.getCurrentKid();

        String accessToken = tokenProvider.createAccessToken(userId, request.getRoles(), jti, device, ip, scopes);
        String refreshToken = tokenProvider.createRefreshToken(userId, jti);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(oauth2Props.getRefreshTokenValiditySeconds());

        var refreshTokenEntity = new Oauth2RefreshTokenRedisEntity(userId, refreshToken, now, expiresAt);
        tokenRepository.storeRefreshToken(refreshTokenEntity);

        log.info("Issued token for userId={} | kid={} | jti={}", userId, kid, jti);
        return new Oauth2AccessToken(accessToken, refreshToken, oauth2Props.getAccessTokenValiditySeconds());
    }
}
