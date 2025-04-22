package org.example.order.core.infra.crypto.algorithm.signer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class HmacSha256Engine {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public static byte[] sign(byte[] message, byte[] key) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, HMAC_ALGORITHM);
        mac.init(secretKeySpec);
        return mac.doFinal(message);
    }
}
