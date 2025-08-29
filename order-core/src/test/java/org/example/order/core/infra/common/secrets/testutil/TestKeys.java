package org.example.order.core.infra.common.secrets.testutil;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public final class TestKeys {

    private static final SecureRandom RNG = new SecureRandom();

    private TestKeys() {
    }

    /**
     * 바이트 길이 기준 랜덤 키 (표준 Base64)
     */
    public static String std(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0");
        }

        byte[] key = new byte[length];
        RNG.nextBytes(key);

        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * 바이트 길이 기준 랜덤 키 (URL-safe Base64, padding 제거)
     */
    public static String url(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0");
        }

        byte[] key = new byte[length];
        RNG.nextBytes(key);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(key);
    }

    /**
     * 가변 파트 ':' 조인 (테스트 문자열 유틸)
     */
    public static String std(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }

        if (parts.length == 1) {
            return Objects.toString(parts[0], "");
        }

        return String.join(":", parts);
    }
}
