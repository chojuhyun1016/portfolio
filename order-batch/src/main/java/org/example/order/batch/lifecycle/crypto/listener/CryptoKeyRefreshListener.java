package org.example.order.batch.lifecycle.crypto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.batch.crypto.selection.CryptoKeySelectionApplier;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * CryptoKeyRefreshListener
 * ------------------------------------------------------------------------
 * 목적
 * - SecretsLoader가 새 키를 로드한 뒤 호출되는 리스너.
 * - 기본 정책: "자동 적용 금지" -> 키 선택은 로드되지만 즉시 승격하지 않음(운영 안정성).
 * - 운영 승인 시점에 별도 경로에서 CryptoKeySelectionApplier.applyAll(true) 호출(수동 승격).
 * <p>
 * 비고
 * - 배치 모듈은 Startup에서 SecretsLoader 스케줄을 취소하므로(주기 갱신 X),
 * 이 리스너가 "초기 1회 시딩"을 보장하는 핵심 고리다.
 * - 암호화/시크릿 인프라가 없는 환경에선 빈 생성을 건너뛰도록 조건부 등록.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean({SecretsLoader.class, CryptoKeySelectionApplier.class})
public class CryptoKeyRefreshListener implements SecretKeyRefreshListener {

    private final CryptoKeySelectionApplier applier;

    @Override
    public void onSecretKeyRefreshed() {
        // 자동 반영 금지(운영 안정성)
        applier.applyAll(false);

        log.info("[Secrets] 키 리프레시 이벤트 수신(자동 적용 금지). 운영 승인 시 별도 경로에서 applyAll(true) 호출 권장.");
    }
}
