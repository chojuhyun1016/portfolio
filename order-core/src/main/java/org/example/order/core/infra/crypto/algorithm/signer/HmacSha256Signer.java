package org.example.order.core.infra.crypto.algorithm.signer;

import lombok.extern.slf4j.Slf4j;
import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Signer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * HMAC-SHA256 Signer
 * - 키는 외부에서 setKey(base64)로 주입
 * - SecretsKeyResolver 등 외부 키 매니저에 의존하지 않음
 * - 서명 결과는 표준 Base64로 인코딩하여 반환
 */
@Slf4j
public class HmacSha256Signer implements Signer {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final byte VERSION = 0x01;

    private byte[] key;

    /**
     * 외부에서 Base64(표준) 키를 주입
     */
    @Override
    public void setKey(String base64Key) {
        try {
            byte[] k = Base64Utils.decode(base64Key);

            if (k == null || k.length == 0) {
                throw new IllegalArgumentException("HMAC-SHA256 key must not be empty.");
            }

            // 권장: 최소 16바이트 이상, 보통 32바이트 사용
            if (k.length < 16) {
                throw new IllegalArgumentException("HMAC-SHA256 key must be at least 16 bytes.");
            }

            this.key = k;

            log.info("[HmacSha256Signer] key set ({} bytes).", k.length);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid HMAC-SHA256 base64 key.", e);
        }
    }

    /**
     * 메시지에 대한 HMAC-SHA256 서명(Base64 표준 인코딩)
     */
    @Override
    public String sign(String message) {
        ensureReady();

        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key, HMAC_ALG));
            byte[] sig = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64Utils.encode(sig);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 signing failed", e);
        }
    }

    /**
     * 서명 검증
     * - 입력 signature는 표준 Base64 문자열을 가정
     */
    @Override
    public boolean verify(String message, String signature) {
        ensureReady();

        try {
            String expected = sign(message);

            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (Exception e) {
            log.warn("[HmacSha256Signer] verify failed: {}", e.getMessage());

            return false;
        }
    }

    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.HMAC_SHA256;
    }

    @Override
    public boolean isReady() {
        return key != null && key.length >= 16;
    }

    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("HMAC-SHA256 signer not initialized. Call setKey(base64) first.");
        }
    }
}
