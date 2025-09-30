package org.example.order.core.infra.common.secrets.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * [요약]
 * - Secrets JSON을 정규화한 내부 엔트리.
 * - Resolver가 alias별로 리스트를 보관하고, 선택 포인터를 관리한다.
 */
@Getter
@AllArgsConstructor
public class CryptoKeySpecEntry {
    private final String alias;      // 예: "order.aesgcm"
    private final String kid;        // 예: "key-2025-09-27"
    private final Integer version;   // 예: 2
    private final String algorithm;  // 예: "AES-256-GCM"
    private final byte[] keyBytes;   // 디코딩 결과
}
