package org.example.order.core.infra.security.oauth2.core.provider;

import org.example.order.core.application.security.vo.Oauth2TokenIssueRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 토큰 생성 시 커스텀 클레임 추가 유틸리티
 */
@Component
public class CustomClaimEnhancer {

    public Map<String, Object> enhance(Oauth2TokenIssueRequest request) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("ip", request.getIpAddress());
        claims.put("clientId", request.getClientId());
        return claims;
    }
}
