package org.example.order.core.infra.lock;

import org.example.order.core.infra.lock.key.impl.SHA256LockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SimpleLockKeyGenerator;
import org.example.order.core.infra.lock.key.impl.SpelLockKeyGenerator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LockKeyGenerator 단위 테스트
 * - generate(String expr, Method method, Object[] args) 시그니처에 맞춰 검증
 */
class KeyGeneratorsUnitTest {

    /**
     * 테스트용 더미 시그니처 홀더
     */
    static class Dummy {
        public void noArgs() {
        }

        public void withArgs(Long id, String text) {
        }
    }

    private static Method mNoArgs() {
        try {
            return Dummy.class.getMethod("noArgs");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method mWithArgs() {
        try {
            return Dummy.class.getMethod("withArgs", Long.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void simple_generates_literal_key() {
        var keygen = new SimpleLockKeyGenerator();

        // 리터럴 결합("'order:' + '42'") → "order:42"
        String key = keygen.generate("'order:' + '42'", mNoArgs(), new Object[0]);

        assertThat(key).isEqualTo("order:42");
    }

    @Test
    void spel_generates_key_from_args() {
        var keygen = new SpelLockKeyGenerator();

        Object[] args = new Object[]{42L, "abc"};
        String expr = "'named:order:' + #p0"; // #p0 ==> 42

        String key = keygen.generate(expr, mWithArgs(), args);

        assertThat(key).isEqualTo("named:order:42");
    }

    @Test
    void sha256_generates_stable_digest() {
        var keygen = new SHA256LockKeyGenerator();

        String src = "payload";
        String k1 = keygen.generate(src, mNoArgs(), new Object[0]);
        String k2 = keygen.generate(src, mNoArgs(), new Object[0]);

        assertThat(k1).isEqualTo(k2);
        assertThat(k1).hasSize(64); // hex(32 bytes) => 64 chars
    }
}
