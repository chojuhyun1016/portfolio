package org.example.order.core.infra.lock.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lock.redisson")
public class RedissonLockProperties {
    private String host;
    private Integer port;
    private String password;
    private int database = 0;
    private long waitTime = 3000;       // ms
    private long leaseTime = 10000;     // ms
    private long retryInterval = 150;   // ms
}
