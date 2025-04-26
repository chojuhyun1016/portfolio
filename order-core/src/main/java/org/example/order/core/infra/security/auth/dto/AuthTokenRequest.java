package org.example.order.core.infra.security.auth.dto;

import lombok.Getter;

@Getter
public class AuthTokenRequest {
    private String refreshToken;
    private String accessToken;
}
