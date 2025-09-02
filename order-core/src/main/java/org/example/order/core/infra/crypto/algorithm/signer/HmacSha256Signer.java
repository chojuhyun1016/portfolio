package org.example.order.core.infra.crypto.algorithm.signer;

import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Signer;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * HMAC-SHA256 Signer
 * <p>
 * - HMAC-SHA256 서명 및 검증 기능 제공
 * - 외부에서 Base64(URL-safe)로 인코딩된 키를 setKey(...)를 통해 주입
 * - SecretsKeyResolver 등의 외부 키 관리 매커니즘에 의존하지 않음
 */
public class HmacSha256Signer implements Signer {

    /**
     * HMAC-SHA256 키 길이 (256-bit = 32 bytes)
     */
    private static final int KEY_LENGTH = 32;

    /**
     * 서명/검증에 사용할 키
     */
    private byte[] key;

    /**
     * Base64(URL-safe)로 인코딩된 키를 외부에서 주입
     *
     * @param base64Key Base64(URL-safe) 형식의 키 문자열
     */
    @Override
    public void setKey(String base64Key) {
        try {
            byte[] k = Base64Utils.decodeUrlSafe(base64Key);
            if (k == null || k.length != KEY_LENGTH) {
                throw new IllegalArgumentException("HMAC-SHA256 key must be exactly 32 bytes.");
            }

            this.key = k;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid HMAC-SHA256 base64 key.", e);
        }
    }

    /**
     * 주어진 메시지에 대해 HMAC-SHA256 서명 생성
     *
     * @param message 서명할 메시지
     * @return Base64(URL - safe) 형식의 서명 문자열
     */
    @Override
    public String sign(String message) {
        ensureReady();

        try {
            byte[] sig = HmacSha256Engine.sign(message.getBytes(StandardCharsets.UTF_8), key);

            return Base64Utils.encodeUrlSafe(sig);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 signing failed", e);
        }
    }

    /**
     * 주어진 메시지와 서명이 일치하는지 검증
     *
     * @param message   원본 메시지
     * @param signature Base64(URL-safe) 형식의 서명
     * @return 서명이 유효하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean verify(String message, String signature) {
        ensureReady();
        try {
            byte[] expected = HmacSha256Engine.sign(message.getBytes(StandardCharsets.UTF_8), key);
            byte[] provided = Base64Utils.decodeUrlSafe(signature);

            return Arrays.equals(expected, provided);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 서명기가 초기화되었는지 여부 확인
     *
     * @return true: 키가 세팅됨 / false: 키 미세팅
     */
    @Override
    public boolean isReady() {
        return key != null && key.length == KEY_LENGTH;
    }

    /**
     * 지원하는 암호화 알고리즘 타입 반환
     *
     * @return CryptoAlgorithmType.HMAC_SHA256
     */
    @Override
    public CryptoAlgorithmType getType() {
        return CryptoAlgorithmType.HMAC_SHA256;
    }

    /**
     * 서명기 사용 전 키가 세팅되어 있는지 확인
     */
    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("HMAC-SHA256 signer not initialized. Call setKey(base64) first.");
        }
    }
}
