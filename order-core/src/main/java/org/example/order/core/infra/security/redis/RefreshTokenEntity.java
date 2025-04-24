package org.example.order.core.infra.security.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("refreshToken")
public class RefreshTokenEntity implements Serializable {
    private String id;
    private String token;
}
