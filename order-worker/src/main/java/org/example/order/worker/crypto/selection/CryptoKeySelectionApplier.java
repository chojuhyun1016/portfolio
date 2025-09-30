package org.example.order.worker.crypto.selection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.crypto.constant.CryptoAlgorithmType;
import org.example.order.core.infra.crypto.contract.Encryptor;
import org.example.order.core.infra.crypto.factory.EncryptorFactory;
import org.example.order.worker.config.properties.AppCryptoKeyProperties;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * CryptoKeySelectionApplier
 * ------------------------------------------------------------------------
 * 목적
 * - Secrets(Resolver)의 alias별 키 스냅샷에서 kid/version 규칙에 맞게 키를 선택하여
 * EncryptorFactory 구현체(AES128/AES256/AESGCM)에 주입.
 * - app.crypto.keys.* 로 “어떤 alias에 어떤 알고리즘을 어떤 키(kid/version)로 고정할지”를 설정.
 * - allowLatest=true 로 호출 시, 설정 누락된 항목은 최신 버전(최대 version)으로 자동 선택(운영 승인용).
 * <p>
 * [핵심]
 * - 선택 규칙: kid 우선 -> version -> (옵션) 최신.
 * - Resolver.applySelection(alias, version, kid, allowLatest) 를 통해 포인터 고정 후 getCurrentKey(alias)로 바이트 취득.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoKeySelectionApplier {

    private final EncryptorFactory factory;
    private final SecretsKeyResolver resolver;
    private final AppCryptoKeyProperties props;

    /**
     * 전체 엔트리를 순회하며 선택 규칙(kid 우선/그다음 version)을 적용해 키를 주입한다.
     *
     * @param allowLatest true면 설정 미존재 시 최신 버전(최대 version) 자동 선택 허용
     */
    public void applyAll(boolean allowLatest) {
        if (props.getKeys() == null || props.getKeys().isEmpty()) {
            log.info("[CryptoKeySelection] app.crypto.keys 비어 있음 -> 스킵");

            return;
        }

        for (Map.Entry<String, AppCryptoKeyProperties.Alias> e : props.getKeys().entrySet()) {
            final String logicalName = e.getKey();
            final AppCryptoKeyProperties.Alias cfg = e.getValue();

            if (cfg == null) {
                log.warn("[CryptoKeySelection] logicalName={} 값이 null -> 스킵", logicalName);

                continue;
            }

            final String alias = cfg.getAlias();
            final String encryptor = cfg.getEncryptor();
            final String kid = cfg.getKid();
            final Integer version = cfg.getVersion();

            if (alias == null || alias.isBlank() || encryptor == null || encryptor.isBlank()) {
                log.warn("[CryptoKeySelection] logicalName={} alias/encryptor 누락 -> 스킵", logicalName);

                continue;
            }

            final CryptoAlgorithmType type;

            try {
                type = CryptoAlgorithmType.valueOf(encryptor);
            } catch (IllegalArgumentException iae) {
                log.error("[CryptoKeySelection] logicalName={} 지원하지 않는 encryptor={}", logicalName, encryptor, iae);

                continue;
            }

            applyOne(logicalName, alias, type, kid, version, allowLatest);
        }
    }

    /**
     * 단일 엔트리 처리
     */
    private void applyOne(String logicalName,
                          String alias,
                          CryptoAlgorithmType type,
                          String preferredKid,
                          Integer preferredVersion,
                          boolean allowLatest) {

        try {
            boolean pinned = resolver.applySelection(alias, preferredVersion, preferredKid, allowLatest);

            if (!pinned) {
                log.warn("[CryptoKeySelection] logicalName={} alias={} 선택 실패(kid/version/최신 불가) -> 스킵",
                        logicalName, alias);

                return;
            }

            byte[] keyBytes = resolver.getCurrentKey(alias);

            Encryptor encryptor = tryGetEncryptor(type);

            if (encryptor == null) {
                log.info("[CryptoKeySelection] logicalName={} alias={} type={} Encryptor 미존재(혹은 Signer/Hasher 대상) -> 스킵",
                        logicalName, alias, type);

                return;
            }

            // Encryptor.setKey 는 base64 문자열을 기대 -> URL-safe base64로 인코딩하여 주입
            String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
            encryptor.setKey(b64);

            log.info("[CryptoKeySelection] logicalName={} alias={} type={} 키 주입 완료", logicalName, alias, type);
        } catch (Exception ex) {
            log.error("[CryptoKeySelection] logicalName={} alias={} 적용 실패", logicalName, alias, ex);
        }
    }

    private Encryptor tryGetEncryptor(CryptoAlgorithmType type) {
        try {
            return factory.getEncryptor(type);
        } catch (Exception ignore) {
            return null;
        }
    }
}
