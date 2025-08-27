package org.example.order.core.infra.common.secrets.testutil;

import java.security.SecureRandom;
import java.util.Base64;

public final class TestKeys {
    private static final SecureRandom RND = new SecureRandom();

    private TestKeys() {
    }

    public static String std(int bytes) {
        byte[] buf = new byte[bytes];
        RND.nextBytes(buf);

        return Base64.getEncoder().encodeToString(buf);
    }

    public static String url(int bytes) {
        byte[] buf = new byte[bytes];
        RND.nextBytes(buf);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
