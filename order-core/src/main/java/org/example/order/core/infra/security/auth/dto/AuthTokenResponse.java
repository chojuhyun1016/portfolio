package org.example.order.core.infra.security.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokenResponse {
    private String accessToken;
    private String refreshToken;
}
