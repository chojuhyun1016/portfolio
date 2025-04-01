package org.example.order.core.redis.support;

import lombok.Builder;
import redis.embedded.RedisServer;

import java.io.IOException;

public class EmbeddedRedis {

    private final EmbeddedRedisConfiguration configuration;
    private RedisServer redisServer;

    @Builder
    public EmbeddedRedis(EmbeddedRedisConfiguration configuration) {
        this.configuration = configuration;
    }

    public void start() throws IOException {
        redisServer = new RedisServer(configuration.getPort());
        redisServer.start();
    }

    public void stop() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
