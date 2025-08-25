package org.example.order.client.kafka.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SSL/SASL 설정
 * - ssl.enabled=true 일 때만 관련 옵션을 클라이언트 설정에 주입
 */
@Getter
@Setter
@ConfigurationProperties("kafka.ssl")
public class KafkaSSLProperties {
    private boolean enabled = false;
    private String securityProtocol;
    private String saslMechanism;
    private String saslJaasConfig;
    private String saslClientCallbackHandlerClass;
}
