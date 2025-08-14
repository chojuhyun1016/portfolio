package org.example.order.core.infra.lock.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NamedLockProperties.class)
@ConditionalOnProperty(name = {"lock.enabled", "lock.named.enabled"}, havingValue = "true", matchIfMissing = false)
public class NamedLockAutoConfig {
    // 프로퍼티 바인딩만 담당 (실행기/빈 등록은 LockModuleConfig에서 수행)
}
