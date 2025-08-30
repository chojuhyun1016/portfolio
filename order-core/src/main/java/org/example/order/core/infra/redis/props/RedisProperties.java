package org.example.order.core.infra.redis.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Redis 단일 프로퍼티 클래스 (수동/자동 공용)
 *
 * - enabled: 전역 스위치 (true 일 때에만 모듈 동작)
 * - uri: redis:// / rediss:// (우선)
 * - host/port: uri 없을 경우 사용
 * - clientName: Lettuce 클라이언트 이름 (명시 시 우선)
 * - enableDefaultClientName: true 일 때 자동 추론 허용 (false 면 clientName 미설정 시 호출 자체 생략)
 * - defaultClientName: 자동 추론 2순위 기본값
 * - trustedPackage: JSON Serializer default typing 신뢰 패키지
 * - 풀/타임아웃 설정: commons-pool2 + Lettuce
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {

    private static final String DEFAULT_TRUSTED_PACKAGE = "org.example.order";

    /** 전역 ON/OFF (기본 false) */
    private Boolean enabled = false;

    /** URI 우선 (예: redis://127.0.0.1:6379, rediss://host:port) */
    private String uri;

    /** host/port 경로 (URI 미설정 시 사용) */
    private String host;
    private Integer port;
    private String password;
    private Integer database;

    /** Lettuce clientName 명시값 (우선순위 1) */
    private String clientName;

    /** clientName 자동추론 사용 여부 (기본 true) */
    private Boolean enableDefaultClientName = true;

    /** 강제 기본 clientName (우선순위 2) */
    private String defaultClientName;

    /** JSON Serializer 신뢰 패키지 (기본 org.example.order) */
    private String trustedPackage = DEFAULT_TRUSTED_PACKAGE;

    /** 타임아웃 (초) */
    private Integer commandTimeoutSeconds = 3;
    private Integer shutdownTimeoutSeconds = 3;

    /** Lettuce 풀 설정 */
    private Integer poolMaxActive = 64;    // max-active
    private Integer poolMaxIdle = 32;      // max-idle
    private Integer poolMinIdle = 8;       // min-idle
    private Long    poolMaxWait   = 2000L; // ms
}
