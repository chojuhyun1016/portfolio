package org.example.order.core.infra.security.oauth2.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Oauth2 클라이언트 설정 관리 클래스
 * - application.yml 등에서 oauth2.client.*로 주입되는 프로퍼티 바인딩
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "oauth2.client")
public class Oauth2ClientProperties {

    private List<Client> clients;

    @Getter
    @Setter
    public static class Client {
        private String clientId;
        private String clientSecret;
        private List<String> scopes;
    }
}
