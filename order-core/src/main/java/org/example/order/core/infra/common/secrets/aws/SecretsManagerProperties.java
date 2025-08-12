package org.example.order.core.infra.common.secrets.aws;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS Secrets Manager 설정 프로퍼티
 * - 자동 로드 방지를 위해 @Configuration 제거 (EnableConfigurationProperties로만 활성화)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "aws.secrets-manager")
public class SecretsManagerProperties {
    private String region;
    private String secretName;
    private long refreshIntervalMillis = 300_000L;  // default 5분
    private boolean failFast = true;                // 실패 시 앱 중단 여부
}
