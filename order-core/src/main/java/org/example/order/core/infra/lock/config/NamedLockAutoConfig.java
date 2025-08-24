package org.example.order.core.infra.lock.config;

import org.example.order.core.infra.lock.props.NamedLockProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * NamedLock 설정 바인딩 전용.
 * - 실제 실행기(Executor) 생성은 LockManualConfig에서 조건부 수행
 */
@Configuration
@EnableConfigurationProperties(NamedLockProperties.class)
@ConditionalOnProperty(name = {"lock.enabled", "lock.named.enabled"}, havingValue = "true", matchIfMissing = false)
public class NamedLockAutoConfig {
    // 프로퍼티 바인딩만 담당
}
