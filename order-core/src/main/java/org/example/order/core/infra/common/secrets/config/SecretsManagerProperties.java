package org.example.order.core.infra.common.secrets.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AWS Secrets Manager 설정 프로퍼티
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws.secrets-manager")
public class SecretsManagerProperties {
    private String region;
    private String secretName;
    private long refreshIntervalMillis = 300_000L;  // default 5분
    private boolean failFast = true;                // 실패 시 앱 중단 여부
}
