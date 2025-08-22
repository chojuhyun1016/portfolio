package org.example.order.core.infra.common.secrets.testutil;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 테스트 전용 키 유틸.
 * - Secrets 계층: 표준 Base64가 기본 (CryptoKeySpec.decodeKey()는 Base64Utils.decode 사용)
 * - 필요 시 URL-safe도 제공 (다른 테스트 재사용 대비)
 */
public final class TestKeys {
    private static final SecureRandom RND = new SecureRandom();

    private TestKeys() {}

    /** 표준 Base64 (예: AES 16B -> 24글자 내외) */
    public static String std(int bytes) {
        byte[] buf = new byte[bytes];
        RND.nextBytes(buf);
        return Base64.getEncoder().encodeToString(buf);
    }

    /** URL-safe Base64 (패딩 제거) */
    public static String url(int bytes) {
        byte[] buf = new byte[bytes];
        RND.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
