package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 큰 맥락
 * - Kafka 클라이언트(Producer/Consumer)에서 SSL 또는 SASL 인증이 필요한 경우 사용하는 설정 클래스.
 * - 외부 설정(application.yml 등)에서 kafka.ssl.* 값을 바인딩한다.
 * - 기본값은 enabled=false 로, 일반 평문 연결 시에는 적용되지 않는다.
 * - enabled=true 인 경우 securityProtocol, saslMechanism 등 추가 옵션을 카프카 클라이언트 설정에 반영한다.
 */
@Getter
@Setter
@ConfigurationProperties("kafka.ssl")
public class KafkaSSLProperties {

    /**
     * SSL/SASL 사용 여부 (기본 false)
     */
    private boolean enabled = false;

    /**
     * 보안 프로토콜 (예: SASL_PLAINTEXT, SASL_SSL 등)
     */
    private String securityProtocol;

    /**
     * SASL 메커니즘 (예: PLAIN, SCRAM-SHA-256 등)
     */
    private String saslMechanism;

    /**
     * SASL JAAS 설정 문자열 (사용자/비밀번호 등 자격 증명 포함)
     */
    private String saslJaasConfig;

    /**
     * SASL 클라이언트 콜백 핸들러 클래스 (필요 시 지정)
     */
    private String saslClientCallbackHandlerClass;
}
