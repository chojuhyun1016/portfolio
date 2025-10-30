package org.example.order.cache.core.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OrderRedisProperties
 * ------------------------------------------------------------------------
 * 목적
 * - order.cache.redis.* 네임스페이스를 통해 Redis 접속/풀/타임아웃 등을
 * 바인딩한다. (spring.redis.* 호환 지원 제거 - B안)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "order.cache.redis")
public class OrderRedisProperties {

    /**
     * 외부에서 재사용할 기본 신뢰 패키지 상수
     */
    public static final String DEFAULT_TRUSTED_PACKAGE = "org.example.order.cache";

    /**
     * URI 우선 (예: redis://, rediss://)
     */
    private String uri;

    /**
     * host/port 경로 (URI 미설정 시 사용)
     */
    private String host;
    private Integer port;
    private String password;
    private Integer database;

    /**
     * Lettuce clientName 명시값 (우선순위 1)
     */
    private String clientName;

    /**
     * clientName 자동추론 사용 여부
     */
    private Boolean enableDefaultClientName;

    /**
     * 강제 기본 clientName (우선순위 2)
     */
    private String defaultClientName;

    /**
     * JSON Serializer 신뢰 패키지
     * - 미설정 시 DEFAULT_TRUSTED_PACKAGE 사용
     */
    private String trustedPackage = DEFAULT_TRUSTED_PACKAGE;

    /**
     * 타임아웃 (초)
     */
    private Integer commandTimeoutSeconds;
    private Integer shutdownTimeoutSeconds;

    /**
     * Lettuce 풀 설정
     */
    private Integer poolMaxActive;   // max-active
    private Integer poolMaxIdle;     // max-idle
    private Integer poolMinIdle;     // min-idle
    private Long poolMaxWait;        // ms
}
