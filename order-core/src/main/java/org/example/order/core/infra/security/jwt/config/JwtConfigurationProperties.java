package org.example.order.core.infra.security.jwt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 설정 프로퍼티
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfigurationProperties {

    /**
     * HMAC Secret Key (Base64 인코딩 추천)
     */
    private String secret;

    /**
     * AccessToken 유효시간 (초)
     */
    private long accessTokenValidityInSeconds;

    /**
     * RefreshToken 유효시간 (초)
     */
    private long refreshTokenValidityInSeconds;
}
