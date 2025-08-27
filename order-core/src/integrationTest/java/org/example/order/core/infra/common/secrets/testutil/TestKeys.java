package org.example.order.core.infra.common.secrets.testutil;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public final class TestKeys {

    private static final SecureRandom RNG = new SecureRandom();

    private TestKeys() {
    }

    public static String std(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }

        if (parts.length == 1) {
            return Objects.toString(parts[0], "");
        }

        return String.join(":", parts);
    }

    public static String std(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0");
        }

        byte[] key = new byte[length];
        RNG.nextBytes(key);

        return Base64.getEncoder().encodeToString(key);
    }
}
