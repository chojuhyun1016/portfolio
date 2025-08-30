package org.example.order.core.infra.common.secrets.props;

import lombok.Getter;
import lombok.Setter;

/**
 * AWS Secrets Manager 설정 프로퍼티
 * - 자동 로드 방지를 위해 @Configuration/@ConfigurationProperties 제거
 * - 설정 클래스(SecretsInfraConfig.Core)에서 @Bean @ConfigurationProperties 로 단일 바인딩
 */
@Getter
@Setter
public class SecretsManagerProperties {

    /**
     * 예: ap-northeast-2
     */
    private String region;

    /**
     * 예: myapp/secret-keyset
     */
    private String secretName;

    /**
     * 기본 5분
     */
    private long refreshIntervalMillis = 300_000L;

    /**
     * 초기 로드 실패 시 앱 중단 여부(운영 권장 true)
     */
    private boolean failFast = true;

    /**
     * 주기 갱신 등록 여부(기본 false) — true + TaskScheduler 빈 존재 시에만 초기 1회 로드 + 주기 갱신 등록
     */
    private boolean schedulerEnabled = false;
}
