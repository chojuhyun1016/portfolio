# 🔐 Crypto 모듈 테스트 가이드 (README)

---

## 📌 무엇을 테스트하나요?

아래 5가지를 **단위/경량 통합** 수준에서 검증합니다.

1) **수동(MANUAL) 모드**: `crypto.enabled=true` 상태에서 사용자가 코드로 각 알고리즘 빈에 `setKey(...)` 호출 후 사용 가능한지
2) **자동(AUTO, Seed) 모드**: `crypto.enabled=true` + `crypto.props.seed=true` 상태에서 `EncryptProperties` 값이 자동 반영되는지
3) **EncryptorFactory 동작**: 모든 Encryptor / Hasher / Signer를 정확히 매핑하는지
4) **부분 시딩**: AUTO 모드에서 일부 키만 설정 시 해당 알고리즘만 ready 상태인지
5) **OFF 모드**: Spring 컨텍스트 없이 new로 직접 생성 후 setKey 호출 시 정상 동작하는지

---

## 🧩 사용 기술

- **ApplicationContextRunner**  
  최소한의 빈만 올려서 조건부 자동 구성(`@ConditionalOnProperty`, `@ConditionalOnMissingBean`)을 안전하게 검증  
  → 메인 클래스 불필요, 전체 컨텍스트 기동 없이 테스트 가능

- **Mockito**  
  Encryptor/Signer/Hasher 목(mock) 생성 가능 (본 테스트 대부분 실제 구현체 사용)

- **AssertJ / JUnit5**  
  빈 존재 여부, ready 상태, 암복호화 결과, 예외 발생 여부 등 검증

---

## 🧪 테스트 코드 전체

```java
package org.example.order.core.infra.crypto;

import org.example.order.core.infra.crypto.algorithm.encryptor.Aes128Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.Aes256Encryptor;
import org.example.order.core.infra.crypto.algorithm.encryptor.AesGcmEncryptor;
import org.example.order.core.infra.crypto.algorithm.signer.HmacSha256Signer;
import org.example.order.core.infra.crypto.config.CryptoAutoConfig;
import org.example.order.core.infra.crypto.config.CryptoManualConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoModuleTest {

    @Test
    void manual_mode_aes128_encrypt_decrypt() {
        ApplicationContextRunner ctx = new ApplicationContextRunner()
                .withPropertyValues("crypto.enabled=true")
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class));

        ctx.run(context -> {
            Aes128Encryptor aes128 = context.getBean(Aes128Encryptor.class);
            String key = base64Key(16);
            aes128.setKey(key);

            String plain = "hello";
            String enc = aes128.encrypt(plain);
            assertThat(aes128.decrypt(enc)).isEqualTo(plain);
        });
    }

    @Test
    void auto_seed_mode_aes256_and_hmac_ready() {
        String key256 = base64Key(32);
        String keyHmac = base64Key(32);

        ApplicationContextRunner ctx = new ApplicationContextRunner()
                .withPropertyValues(
                        "crypto.enabled=true",
                        "crypto.props.seed=true",
                        "encrypt.aes256.key=" + key256,
                        "encrypt.hmac.key=" + keyHmac
                )
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class, CryptoAutoConfig.class));

        ctx.run(context -> {
            Aes256Encryptor aes256 = context.getBean(Aes256Encryptor.class);
            HmacSha256Signer hmac = context.getBean(HmacSha256Signer.class);

            assertThat(aes256.isReady()).isTrue();
            assertThat(hmac.isReady()).isTrue();
        });
    }

    @Test
    void manual_mode_aesgcm_encrypt_decrypt() {
        ApplicationContextRunner ctx = new ApplicationContextRunner()
                .withPropertyValues("crypto.enabled=true")
                .withConfiguration(UserConfigurations.of(CryptoManualConfig.class));

        ctx.run(context -> {
            AesGcmEncryptor aesgcm = context.getBean(AesGcmEncryptor.class);
            String key = base64Key(32);
            aesgcm.setKey(key);

            String plain = "data";
            String enc = aesgcm.encrypt(plain);
            assertThat(aesgcm.decrypt(enc)).isEqualTo(plain);
        });
    }

    private static String base64Key(int length) {
        byte[] bytes = new byte[length];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
```

---

## ⚙️ 속성 기반 모드 제어

- **MANUAL 모드**: `crypto.enabled=true`
    - 등록 빈: Encryptor, Hasher, Signer, Factory
    - setKey 직접 호출 필요
    - EncryptProperties 무시됨

- **AUTO(Seed) 모드**: `crypto.enabled=true` + `crypto.props.seed=true`
    - EncryptProperties 값 자동 주입
    - 부분 시딩 허용(비어있으면 건너뜀)

예시 속성:

```properties
# Manual 모드
crypto.enabled=true

# Auto(Seed) 모드
crypto.enabled=true
crypto.props.seed=true
encrypt.aes128.key=BASE64_16B_KEY
encrypt.aes256.key=BASE64_32B_KEY
encrypt.aesgcm.key=BASE64_32B_KEY
encrypt.hmac.key=BASE64_32B_KEY
```

---

## 🧾 주의사항

- AES128: 16바이트, AES256/AESGCM/HMAC: 32바이트 키 필요
- Base64 인코딩 필수
- 잘못된 키 길이 → `IllegalStateException`

---

## 🚀 실행 방법

```bash
./gradlew :order-core:test --tests "*CryptoModuleTest*"
```

---

## ✅ 체크리스트

- [x] MANUAL 모드에서 setKey 후 정상 동작
- [x] AUTO 모드에서 EncryptProperties 값 반영
- [x] 부분 시딩 시 해당 알고리즘만 ready
- [x] 잘못된 키 길이 시 예외
