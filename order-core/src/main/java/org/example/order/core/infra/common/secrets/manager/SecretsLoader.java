package org.example.order.core.infra.common.secrets.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.infra.json.ObjectMapperUtils;
import org.example.order.core.infra.common.secrets.config.SecretsManagerProperties;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.List;
import java.util.Map;

/**
 * AWS Secrets Manager에서 주기적으로 JSON 키셋을 가져와 파싱 + Resolver에 등록.
 * 리스너를 통해 다른 매니저들에게 키 변경 알림을 전달.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecretsLoader {

    private final SecretsManagerProperties properties;
    private final SecretsKeyResolver secretsKeyResolver;
    private final SecretsManagerClient secretsManagerClient;
    private final List<SecretKeyRefreshListener> refreshListeners;  // 새로운 리스너 목록

    @PostConstruct
    public void init() {
        refreshSecrets();
    }

    @Scheduled(fixedDelayString = "${aws.secrets-manager.refresh-interval-millis:300000}")
    public void refreshSecrets() {
        try {
            var request = GetSecretValueRequest.builder()
                    .secretId(properties.getSecretName())
                    .build();

            var response = secretsManagerClient.getSecretValue(request);
            String secretJson = response.secretString();

            // Map<String, CryptoKeySpec> 변환
            Map<String, CryptoKeySpec> parsedKeys = ObjectMapperUtils.readValue(
                    secretJson,
                    new TypeReference<>() {}
            );

            parsedKeys.forEach((keyName, spec) -> {
                byte[] decoded = spec.decodeKey();
                int expectedBytes = spec.getKeySize() / 8;

                if (decoded.length != expectedBytes) {
                    throw new IllegalArgumentException(
                            String.format("Invalid key size for [%s]: expected %d bytes, got %d",
                                    keyName, expectedBytes, decoded.length)
                    );
                }
                secretsKeyResolver.updateKey(keyName, spec);
            });

            log.info("[SecretsLoader] Keys refreshed successfully. Total: {}", parsedKeys.size());

            // 리스너에게 알림 전달
            refreshListeners.forEach(listener -> {
                try {
                    listener.onSecretKeyRefreshed();
                    log.info("[SecretsLoader] SecretKeyRefreshListener [{}] notified.", listener.getClass().getSimpleName());
                } catch (Exception ex) {
                    log.error("[SecretsLoader] Listener {} failed during onSecretKeyRefreshed: {}",
                            listener.getClass().getSimpleName(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("[SecretsLoader] Failed to refresh secrets.", e);
            if (properties.isFailFast()) {
                throw new IllegalStateException("Secret loading failed. Aborting app startup.", e);
            }
        }
    }
}
