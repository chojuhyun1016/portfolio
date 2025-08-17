package org.example.order.core.infra.redis.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Redis 단일 프로퍼티 클래스 (수동/자동 공용)
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

    private static final String DEFAULT_TRUSTED_PACKAGE = "org.example.order";

    private Boolean enabled;
    private String uri;

    private String host;
    private Integer port;
    private String password;
    private Integer database;

    /**
     * Lettuce clientName 명시값 (우선순위 1)
     * - 비어있거나 null이면 아래 자동 추론 로직으로 처리됨
     */
    private String clientName;

    /**
     * clientName 자동추론 사용 여부 (기본 true)
     * - false 이면, clientName 이 비어있을 때 아예 clientName 설정을 호출하지 않음
     */
    private Boolean enableDefaultClientName = true;

    /**
     * 강제 기본 clientName (우선순위 2)
     * - clientName 미설정이고 enableDefaultClientName=true 일 때 적용
     */
    private String defaultClientName;

    private String trustedPackage = DEFAULT_TRUSTED_PACKAGE;

    private Integer commandTimeoutSeconds = 3;
    private Integer shutdownTimeoutSeconds = 3;

    /**
     * Lettuce 풀 설정 (기본값: commons-pool2)
     */
    private Integer poolMaxActive = 64;    // max-active
    private Integer poolMaxIdle = 32;      // max-idle
    private Integer poolMinIdle = 8;       // min-idle
    private Long poolMaxWait = 2000L;      // ms
}
