package org.example.order.core.infra.common.secrets.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SecretsManagerProperties
 * <p>
 * - 공통: aws.region / aws.endpoint (최상위)
 * - 전용: aws.secrets-manager.* (아래 Secrets 블록)
 * - 자격증명: aws.credential.* (최상위 공통 – S3와 공유)
 * <p>
 * 예시(application-*.yml)
 * aws:
 * region: ap-northeast-2
 * endpoint: http://localhost:4566         # LocalStack일 때만 지정, 운영은 비움
 * credential:
 * enabled: true
 * access-key: local
 * secret-key: local
 * secrets-manager:
 * enabled: true
 * secret-name: myapp/secret-key
 * scheduler-enabled: true
 * refresh-interval-millis: 300000
 * fail-fast: true
 */
@Getter
@Setter
@ConfigurationProperties("aws")
public class SecretsManagerProperties {

    /**
     * 공통(최상위)
     */
    private String region;
    private String endpoint;

    private Credential credential = new Credential();
    private Secrets secretsManager = new Secrets();

    @Getter
    @Setter
    public static class Credential {
        /**
         * true → AccessKey/SecretKey를 정적으로 사용(예: LocalStack)
         * false → IAM Role/DefaultCredentialsProvider 사용(운영 권장)
         */
        private boolean enabled = false;
        private String accessKey;
        private String secretKey;
    }

    @Getter
    @Setter
    public static class Secrets {
        /**
         * 시크릿 매니저 모듈 ON/OFF
         */
        private boolean enabled = false;

        /**
         * 필수: Secret 이름
         */
        private String secretName;

        /**
         * 로딩/검증/스케줄 옵션들
         */
        private long refreshIntervalMillis = 300_000L;
        private boolean failFast = true;
        private boolean schedulerEnabled = false;
    }
}
