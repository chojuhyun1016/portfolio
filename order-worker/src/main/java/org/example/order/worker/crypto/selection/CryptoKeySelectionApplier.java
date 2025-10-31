package org.example.order.worker.crypto.selection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.helper.encode.Base64Utils;
import org.example.order.core.infra.common.secrets.client.SecretsKeyClient;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.contract.Signer;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.example.order.worker.config.properties.AppCryptoKeyProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CryptoKeySelectionApplier
 * ------------------------------------------------------------------------
 * 목적
 * - app.crypto.keys 설정을 읽어 alias/kid/version 기준으로 Secrets에서 키를 선택(apply)하고,
 * EncryptorFactory/Signer에 실제 키를 시딩한다(Encryptor/Signer 대상에 한함).
 * <p>
 * 정책
 * - 운영 기본: 자동 최신 금지(allowLatest=false). 승인 시 true로 수동 승격 가능.
 * - 설정의 encryptor 표기는 사람 친화적 포맷(AES-GCM, aes_256 등) 허용 → 내부 enum으로 정규화.
 * - Key 전달 시 **표준 Base64** 문자열로 통일(내부 구현체가 표준 Base64 디코드 가정).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoKeySelectionApplier {

    private final AppCryptoKeyProperties props;
    private final SecretsKeyClient secrets;
    private final EncryptorFactory factory;

    /**
     * 전체 적용
     */
    public void applyAll(boolean allowLatest) {
        for (Map.Entry<String, AppCryptoKeyProperties.Alias> e : props.getKeys().entrySet()) {
            String logicalName = e.getKey();
            AppCryptoKeyProperties.Alias a = e.getValue();
            applyOne(logicalName, a, allowLatest);
        }
    }

    /**
     * 단건 적용
     */
    private void applyOne(String logicalName, AppCryptoKeyProperties.Alias a, boolean allowLatest) {
        String alias = a.getAlias();
        String encStr = a.getEncryptor();
        String kid = a.getKid();
        Integer version = a.getVersion();

        // 1) encryptor 타입 정규화
        final CryptoAlgorithmType type;
        try {
            type = normalizeAlgorithm(encStr);
        } catch (IllegalArgumentException ex) {
            log.error("[CryptoKeySelection] logicalName={} 지원하지 않는 encryptor={}", logicalName, encStr, ex);

            return;
        }

        // 2) Secrets에서 선택 고정(kid -> version -> (allowLatest면) 최신)
        boolean selected = secrets.applySelection(alias, version, kid, allowLatest);
        if (!selected) {
            log.warn("[CryptoKeySelection] logicalName={} alias={} 선택 실패(kid/version/최신 불가) -> 스킵",
                    logicalName, alias);

            return;
        }

        // 3) 현재 선택 키 바이트 획득
        final byte[] keyBytes;

        try {
            keyBytes = secrets.getKey(alias);
        } catch (IllegalStateException ise) {
            log.warn("[CryptoKeySelection] logicalName={} alias={} 현재 키 없음 -> 스킵", logicalName, alias);

            return;
        }

        // 4) 대상별 키 시딩
        switch (type) {
            // === 대칭키 Encryptor 대상 ===
            case AES128, AES256, AESGCM -> {
                Encryptor encryptor = factory.getEncryptor(type);
                encryptor.setKey(Base64Utils.encode(keyBytes));

                log.info("[CryptoKeySelection] logicalName={} alias={} type={} 키 시딩 완료", logicalName, alias, type);
            }

            // === MAC/서명(Signature) 대상 ===
            case HMAC_SHA256 -> {
                Signer signer = factory.getSigner(type);
                signer.setKey(Base64Utils.encode(keyBytes));

                log.info("[CryptoKeySelection] logicalName={} alias={} type={} 키 시딩 완료", logicalName, alias, type);
            }

            // === 해시(비밀키 불필요) 대상: 스킵 ===
            case SHA256, SHA512, BCRYPT, ARGON2 -> {
                log.info("[CryptoKeySelection] logicalName={} alias={} type={} 해시/단방향 -> 시딩 스킵",
                        logicalName, alias, type);
            }
        }
    }

    /**
     * 설정의 encryptor 문자열을 내부 enum으로 정규화
     * - 하이픈/언더스코어/공백/슬래시/점 제거 후 대문자
     * - 흔한 별칭을 간결히 매핑(AESGCM, AES256, AES128, SHA256, SHA512, HMACSHA256)
     */
    private static CryptoAlgorithmType normalizeAlgorithm(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("encryptor is null");
        }

        String t = raw.trim().toUpperCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "")
                .replace("/", "")
                .replace(".", "");

        Map<String, String> alias = new HashMap<>();
        alias.put("AESGCM", "AESGCM");
        alias.put("AES256", "AES256");
        alias.put("AES128", "AES128");
        alias.put("SHA256", "SHA256");
        alias.put("SHA512", "SHA512");
        alias.put("HMACSHA256", "HMAC_SHA256");

        String canonical = alias.getOrDefault(t, t);

        return CryptoAlgorithmType.valueOf(canonical);
    }
}
