package org.example.order.core.lock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lock.named")
public class NamedLockProperties {
    private int waitTime = 3000;        // 최대 대기 시간 (ms)
    private int retryInterval = 100;    // 재시도 간격 (ms)
}
