package org.example.order.core.application.security.response;

/**
 * 액세스 토큰 응답 모델.
 */
public record Oauth2AccessToken(String accessToken, String refreshToken, int expiresIn) {
}
