package org.example.order.core.infra.lock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lock.redisson")
public class RedissonProperties {
    private String address;
    private String password;
    private int database = 0;
    private long waitTime = 3000;       // ms
    private long leaseTime = 10000;     // ms
    private long retryInterval = 150;   // ms
}
