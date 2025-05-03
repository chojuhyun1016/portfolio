package org.example.order.core.infra.security.gateway.config;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.gateway.matcher.WhiteListMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 게이트웨이 설정:
 * - 화이트리스트 매처 Bean 등록 (GatewaySecurityProperties 기반)
 * - HEALTH_CHECK_PATH 고정 포함 (Kubernetes Probe 대응)
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    protected static final String HEALTH_CHECK_PATH = "/actuator/health";

    private final GatewaySecurityProperties securityProperties;

    @Bean
    public WhiteListMatcher whiteListMatcher() {
        List<String> whiteList = new ArrayList<>(securityProperties.getWhitelist());

        if (!whiteList.contains(HEALTH_CHECK_PATH)) {
            whiteList.add(HEALTH_CHECK_PATH);
        }

        return new WhiteListMatcher(whiteList);
    }
}
