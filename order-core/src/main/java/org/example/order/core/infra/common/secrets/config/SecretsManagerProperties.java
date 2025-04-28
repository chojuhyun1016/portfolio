package org.example.order.core.infra.common.secrets.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AWS Secrets Manager 설정
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aws.secrets-manager")
public class SecretsManagerProperties {
    private String region;                          // AWS 리전
    private String secretName;                      // Secret 이름
    private long refreshIntervalMillis = 300_000L;  // 키 갱신 주기 (기본 5분)
}
