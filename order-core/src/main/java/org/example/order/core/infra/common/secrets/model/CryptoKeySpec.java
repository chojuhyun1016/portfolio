package org.example.order.core.infra.common.secrets.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.order.common.helper.encode.Base64Utils;

/**
 * [요약]
 * - Secrets JSON의 단일 키 항목 DTO.
 * - { kid, version, algorithm, key } 필수/선택 스펙 반영.
 * <p>
 * [핵심 포인트]
 * - key(Base64)만 디코딩 책임을 가짐. 나머지 해석은 로더/리졸버에서 수행.
 */
@Getter
@Setter
@ToString
public class CryptoKeySpec {
    private String kid;        // 선택: KID
    private Integer version;   // 선택: 정수 버전
    private String algorithm;  // 필수: "AES-256-GCM" 등
    private String key;        // 필수: Base64(또는 URL-safe)

    public byte[] decodeKey() {
        return Base64Utils.decodeFlexible(key);
    }
}
