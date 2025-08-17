package org.example.order.core.infra.lock.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "lock.named")
public class NamedLockProperties {
    private int waitTime = 3000;        // 최대 대기 시간 (ms)
    private int retryInterval = 150;    // 재시도 간격 (ms)
}
