package org.example.order.core.infra.security.oauth2.core.contract;

/**
 * Oauth2 클라이언트 정보 조회 서비스
 * - 구현 쪽에서 clientId 기반으로 메타데이터 조회만 처리
 */
public interface Oauth2ClientService {

    /**
     * 주어진 clientId로 Client 메타 정보 조회
     *
     * @param clientId 클라이언트 ID
     * @return 메타 정보(JSON 또는 Custom DTO 등 구현 쪽 결정)
     */
    Object loadClientById(String clientId);
}
