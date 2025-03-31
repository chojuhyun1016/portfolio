package org.example.order.core.crypto.engine;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class HmacSha256Engine {

    private static final String ALGORITHM = "HmacSHA256";

    public static byte[] sign(String message, String key) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));

        return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }
}
