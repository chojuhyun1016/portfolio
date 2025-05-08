package org.example.order.core.application.security.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.Instant;

/**
 * Oauth2 RefreshToken Redis 엔티티 (발급일, 만료일 포함).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("oauth2_refresh_token")
public class Oauth2RefreshTokenRedisEntity implements Serializable {

    private String userId;         // 사용자 ID
    private String tokenValue;     // RefreshToken 값
    private Instant issuedAt;      // 발급일시
    private Instant expiresAt;     // 만료일시
}
