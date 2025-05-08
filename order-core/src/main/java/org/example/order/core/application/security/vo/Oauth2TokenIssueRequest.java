package org.example.order.core.application.security.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * OAuth2 토큰 요청 모델
 */
@Getter
@Builder
public class Oauth2TokenIssueRequest {

    private final String userId;               // 사용자 ID
    private final List<String> roles;          // 권한 (ROLE_USER 등)
    private final List<String> scopes;         // OAuth 스코프
    private final String deviceId;             // 디바이스 ID (ex: User-Agent 또는 고유 식별자)
    private final String ipAddress;            // 클라이언트 IP 주소
    private final String clientId;             // OAuth 클라이언트 ID (앱 식별자)
}
