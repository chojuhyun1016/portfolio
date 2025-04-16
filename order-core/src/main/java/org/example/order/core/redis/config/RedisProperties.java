package org.example.order.core.redis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

    private static final String DEFAULT_TRUSTED_PACKAGE = "org.example.order";

    private String host;
    private int port;
    private String password;
    private String trustedPackage = DEFAULT_TRUSTED_PACKAGE;
}
