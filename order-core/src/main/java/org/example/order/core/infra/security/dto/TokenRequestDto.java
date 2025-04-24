package org.example.order.core.infra.security.dto;

import lombok.Getter;

@Getter
public class TokenRequestDto {
    private String refreshToken;
    private String accessToken;
}
