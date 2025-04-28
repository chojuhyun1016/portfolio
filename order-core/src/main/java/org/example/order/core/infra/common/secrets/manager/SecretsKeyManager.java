package org.example.order.core.infra.common.secrets.manager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.secrets.config.SecretsManagerProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.Base64;

/**
 * Secrets Manager로부터 Secret Key를 주기적으로 로딩하는 매니저
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecretsKeyManager {

    private final SecretsManagerProperties properties;
    private final SecretsKeyResolver secretsKeyResolver;
    private final SecretsManagerClient secretsManagerClient;

    /**
     * 애플리케이션 부팅 시 최초 키 로드
     */
    @PostConstruct
    public void init() {
        refreshKey();
    }

    /**
     * 주기적으로 Secrets Manager에서 키를 새로 로드하여 핫스왑
     */
    @Scheduled(fixedDelayString = "${aws.secrets-manager.refresh-interval-millis:300000}")
    public void refreshKey() {
        try {
            var request = GetSecretValueRequest.builder()
                    .secretId(properties.getSecretName())
                    .build();

            var response = secretsManagerClient.getSecretValue(request);
            byte[] newKey = Base64.getDecoder().decode(response.secretString());

            secretsKeyResolver.updateKey(newKey);
            log.info("[SecretsKeyManager] Secret key refreshed and hot swapped successfully.");
        } catch (Exception e) {
            log.error("[SecretsKeyManager] Failed to refresh secret key", e);
        }
    }
}
