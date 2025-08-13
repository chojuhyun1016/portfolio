package org.example.order.core.infra.common.secrets.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.order.common.helper.encode.Base64Utils;

/**
 * 암호화 키 스펙 모델 (JSON 내 각 키 항목)
 */
@Getter
@Setter
@ToString
public class CryptoKeySpec {
    private String algorithm;   // 예: AES, AES-GCM, HMAC-SHA256
    private int keySize;        // 예: 128, 256
    private String value;       // Base64(또는 URL-safe Base64) 인코딩 키

    public byte[] decodeKey() {
        return Base64Utils.decode(value);
    }
}
