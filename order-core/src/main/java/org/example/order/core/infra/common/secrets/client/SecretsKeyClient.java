package org.example.order.core.infra.common.secrets.client;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpecEntry;

import java.util.List;

/**
 * [요약]
 * - 리졸버에 대한 얇은 래퍼.
 * - 스냅샷 주입, 선택 정책 적용, 현재 키/특정 버전 키 조회를 단순화.
 */
@RequiredArgsConstructor
public class SecretsKeyClient {
    private final SecretsKeyResolver resolver;

    public void setSnapshot(String alias, List<CryptoKeySpecEntry> entries) {
        resolver.setSnapshot(alias, entries);
    }

    public byte[] getKey(String alias) {
        return resolver.getCurrentKey(alias);
    }

    public byte[] getKey(String alias, Integer version, String kid) {
        return resolver.getKey(alias, version, kid);
    }

    public boolean applySelection(String alias, Integer version, String kid, boolean allowLatest) {
        return resolver.applySelection(alias, version, kid, allowLatest);
    }
}
