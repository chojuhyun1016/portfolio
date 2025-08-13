package org.example.order.core.infra.common.secrets.client;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;

/**
 * SecretsKeyClient
 * - 사용자가 코드에서 키를 편하게 set/get 할 수 있도록 제공하는 얇은 래퍼
 * - 운영(AWS)에서도 같은 인터페이스로 조회 가능(로더가 Resolver를 채움)
 */
@RequiredArgsConstructor
public class SecretsKeyClient {

    private final SecretsKeyResolver resolver;

    /**
     * 키 등록/업데이트 (핫스왑 + 백업 유지)
     */
    public void setKey(String keyName, CryptoKeySpec spec) {
        resolver.updateKey(keyName, spec);
    }

    /**
     * 현재 키 조회 (없으면 IllegalStateException)
     */
    public byte[] getKey(String keyName) {
        return resolver.getCurrentKey(keyName);
    }

    /**
     * 백업 키 조회 (없으면 null)
     */
    public byte[] getBackupKey(String keyName) {
        return resolver.getBackupKey(keyName);
    }
}
