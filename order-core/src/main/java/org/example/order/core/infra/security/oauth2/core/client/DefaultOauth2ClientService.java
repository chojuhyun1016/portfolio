package org.example.order.core.infra.security.oauth2.core.client;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.security.oauth2.config.Oauth2ClientProperties;
import org.example.order.core.infra.security.oauth2.core.contract.Oauth2ClientService;
import org.example.order.core.application.security.vo.Oauth2ClientMetadata;
import org.springframework.stereotype.Service;

/**
 * Oauth2 클라이언트 정보 조회 서비스 기본 구현
 */
@Service
@RequiredArgsConstructor
public class DefaultOauth2ClientService implements Oauth2ClientService {

    private final Oauth2ClientProperties clientProperties;

    @Override
    public Oauth2ClientMetadata loadClientById(String clientId) {
        return clientProperties.getClients().stream()
                .filter(client -> client.getClientId().equals(clientId))
                .findFirst()
                .map(client -> new Oauth2ClientMetadata(
                        client.getClientId(),
                        client.getClientSecret(),
                        client.getScopes()
                ))
                .orElse(null);
    }
}
