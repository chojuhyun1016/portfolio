package org.example.order.core.application.security.vo;

import java.util.List;

/**
 * Oauth2 클라이언트 세부 정보.
 */
public record Oauth2ClientMetadata(String clientId, String clientSecret, List<String> scopes) {
}
