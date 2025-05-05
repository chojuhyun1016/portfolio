package org.example.order.core.infra.security.oauth2.core.contract;

import org.example.order.core.application.security.vo.Oauth2ClientMetadata;

/**
 * Oauth2 클라이언트 인증 서비스 인터페이스
 */
public interface Oauth2ClientService {

    Oauth2ClientMetadata loadClientById(String clientId);
}
