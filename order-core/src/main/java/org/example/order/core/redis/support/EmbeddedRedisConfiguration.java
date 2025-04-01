package org.example.order.core.redis.support;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmbeddedRedisConfiguration {
    private int port;
}
