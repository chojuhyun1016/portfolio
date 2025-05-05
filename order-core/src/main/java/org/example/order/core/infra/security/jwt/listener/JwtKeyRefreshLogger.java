package org.example.order.core.infra.security.jwt.listener;

import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.springframework.stereotype.Component;

/**
 * 가 새로 로드될 때 로깅하는 리스너 (테스트/모니터링용)
 */
@Slf4j
@Component
public class JwtKeyRefreshLogger implements SecretKeyRefreshListener {

    @Override
    public void onSecretKeyRefreshed() {
        log.info("[JwtKeyRefreshLogger] JWT signing key refreshed from SecretsManager.");
    }
}
