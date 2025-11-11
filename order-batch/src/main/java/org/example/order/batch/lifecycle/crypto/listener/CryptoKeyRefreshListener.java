package org.example.order.batch.lifecycle.crypto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.batch.crypto.selection.CryptoKeySelectionApplier;
import org.springframework.stereotype.Component;

/**
 * CryptoKeyRefreshListener
 * ------------------------------------------------------------------------
 * 목적
 * - SecretsLoader가 새 키를 로드한 뒤 호출되는 리스너
 * - 기본 정책: "자동 적용 금지" -> 키 선택은 로드되지만 즉시 승격하지 않음(운영 안정성)
 * - 운영 승인 시점에 별도 경로에서 CryptoKeySelectionApplier.applyAll(true) 호출(수동 승격)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoKeyRefreshListener implements SecretKeyRefreshListener {

    private final CryptoKeySelectionApplier applier;

    @Override
    public void onSecretKeyRefreshed() {
        // 자동 반영 금지(운영 안정성)
        applier.applyAll(false);

        log.info("[Secrets] 키 리프레시 이벤트 수신(자동 적용 금지). 운영 승인 시 별도 경로에서 applyAll(true) 호출 권장.");
    }
}
