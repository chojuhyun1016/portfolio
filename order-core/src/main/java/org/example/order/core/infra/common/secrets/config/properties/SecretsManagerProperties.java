package org.example.order.core.infra.common.secrets.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [요약]
 * - aws.* 공통 옵션 + aws.secrets-manager.* 세부 옵션을 바인딩.
 * - endpoint가 지정되면 LocalStack 등 테스트 환경으로 간주.
 * <p>
 * [핵심 포인트]
 * - fail-fast: 초기 로드 실패 시 운영에선 즉시 중단(권장), 로컬/LocalStack은 완화.
 */
@Getter
@Setter
@ConfigurationProperties("aws")
public class SecretsManagerProperties {

    private String region;
    private String endpoint;

    private Credential credential = new Credential();
    private Secrets secretsManager = new Secrets();

    @Getter
    @Setter
    public static class Credential {
        private boolean enabled = false;
        private String accessKey;
        private String secretKey;
    }

    @Getter
    @Setter
    public static class Secrets {
        private boolean enabled = false;
        private String secretName;
        private long refreshIntervalMillis = 300_000L;
        private boolean failFast = true;
        private boolean schedulerEnabled = false;
    }
}
