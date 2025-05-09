package org.example.order.core.infra.security.oauth2.core.contract;

import org.example.order.core.application.security.response.Oauth2AccessToken;
import org.example.order.core.application.security.vo.Oauth2TokenIssueRequest;

/**
 * Oauth2 토큰 발급 및 검증 인터페이스
 */
public interface Oauth2TokenProvider {

    Oauth2AccessToken createAccessToken(Oauth2TokenIssueRequest request);

    Oauth2AccessToken refreshAccessToken(String userId, String refreshToken);

    boolean validateAccessToken(String token);

    boolean validateRefreshToken(String userId, String refreshToken);
}
