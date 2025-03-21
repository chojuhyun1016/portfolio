package org.example.order.client.kafka.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
