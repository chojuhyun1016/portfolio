package org.example.order.worker.lifecycle.crypto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.worker.crypto.selection.CryptoKeySelectionApplier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * ApplicationCryptoKeyRefreshListenerImpl
 * ------------------------------------------------------------------------
 * 목적
 * - SecretsLoader가 새 키를 로드한 뒤 호출되는 리스너.
 * - 기본은 "자동 적용 금지" -> 로그만 남김.
 * - 운영 승인 시점에 별도 경로에서 CryptoKeySelectionApplier.applyAll(true) 호출(수동 승격).
 */
@Slf4j
@Component
@Profile({"local", "dev", "beta", "prod"})
@RequiredArgsConstructor
public class ApplicationCryptoKeyRefreshListenerImpl implements SecretKeyRefreshListener {

    private final CryptoKeySelectionApplier applier;

    @Override
    public void onSecretKeyRefreshed() {
        // 자동 반영 금지(운영 안정성)
        // applier.applyAll(true);
        log.info("[Secrets] 키 리프레시 이벤트 수신(자동 적용 금지). 운영 승인 시 별도 경로에서 applyAll(true) 호출 권장.");
    }
}
