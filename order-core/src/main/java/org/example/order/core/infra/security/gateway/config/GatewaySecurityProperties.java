package org.example.order.core.infra.security.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 게이트웨이 보안 관련 설정 (화이트리스트 포함)
 * - application.yml의 custom.security.* 키와 매핑됩니다.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "custom.security")
public class GatewaySecurityProperties {
    private List<String> whitelist;
}
