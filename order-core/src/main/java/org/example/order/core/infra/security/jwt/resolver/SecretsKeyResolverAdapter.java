package org.example.order.core.infra.security.jwt.resolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.security.jwt.contract.TokenProvider;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 * SecretsKeyResolver 기반 KeyResolver + KidProvider 구현체
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecretsKeyResolverAdapter implements TokenProvider.KeyResolver, TokenProvider.KidProvider {

    private static final String DEFAULT_KEY_NAME = "jwt-signing-key";

    private final SecretsKeyResolver secretsKeyResolver;

    @Override
    public Key resolveKey(String token) {
        byte[] keyBytes = secretsKeyResolver.getCurrentKey(DEFAULT_KEY_NAME);

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Override
    public String getCurrentKid() {
        return DEFAULT_KEY_NAME;  // 필요 시 동적 kid 가능
    }
}
