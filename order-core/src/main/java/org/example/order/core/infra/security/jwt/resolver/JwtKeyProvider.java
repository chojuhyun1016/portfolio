package org.example.order.core.infra.security.jwt.resolver;

import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Objects;

/**
 * 인메모리 JWT 키 공급자
 * - 외부에서 setKey(byte[]/base64URL)로 서명키를 주입해서 사용
 * - SecretsKeyResolver, 프로퍼티 등에 의존하지 않음
 * - 필요 시 @Configuration에서 Bean으로 등록해 두 인터페이스(KeyResolver, KidProvider)로 주입
 */
public class JwtKeyProvider implements TokenProvider.KeyResolver, TokenProvider.KidProvider {

    private volatile byte[] keyBytes;
    private volatile String kid = "jwt-key";

    /**
     * Base64(URL-safe) 키 문자열로 설정
     */
    public JwtKeyProvider setBase64UrlKey(String base64Url) {
        this.keyBytes = Base64Utils.decodeUrlSafe(Objects.requireNonNull(base64Url, "base64Url"));
        return this;
    }

    /**
     * 바이너리 키로 설정
     */
    public JwtKeyProvider setKey(byte[] keyBytes) {
        this.keyBytes = Objects.requireNonNull(keyBytes, "keyBytes");
        return this;
    }

    /**
     * 현재 kid 설정 (옵션)
     */
    public JwtKeyProvider setKid(String kid) {
        this.kid = Objects.requireNonNull(kid, "kid");
        return this;
    }

    @Override
    public Key resolveKey(String token) {
        if (keyBytes == null || keyBytes.length == 0) {
            throw new IllegalStateException("JWT signing key is not set. Call setKey(...) first.");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Override
    public String getCurrentKid() {
        return kid;
    }
}
