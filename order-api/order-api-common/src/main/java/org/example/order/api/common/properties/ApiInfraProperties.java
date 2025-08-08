package org.example.order.api.common.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * API/MASTER 공통 인프라 설정.
 * - 각 서비스는 application.yml 에서 order.api.infra.* 만 채우면 됨
 */
@ConfigurationProperties(prefix = "order.api.infra")
public class ApiInfraProperties {

    public record Logging(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("/*") List<String> includePatterns,
            @DefaultValue({"/actuator/*"}) List<String> excludePatterns,
            @DefaultValue("1048576") int maxPayloadLength // 1MB
    ) {
    }

    public record Security(
            @DefaultValue("true") boolean enabled,
            Gateway gateway,
            @DefaultValue({"/actuator/health", "/actuator/info"}) List<String> permitAllPatterns,
            @DefaultValue({"/**"}) List<String> authenticatedPatterns
    ) {
    }

    public record Gateway(
            @DefaultValue("X-Internal-Auth") String header,
            String secret // env/secret 로 주입
    ) {
    }

    private Logging logging = new Logging(true, List.of("/*"), List.of("/actuator/*"), 1_048_576);
    private Security security = new Security(true, new Gateway("X-Internal-Auth", null),
            List.of("/actuator/health", "/actuator/info"), List.of("/**"));

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }
}
