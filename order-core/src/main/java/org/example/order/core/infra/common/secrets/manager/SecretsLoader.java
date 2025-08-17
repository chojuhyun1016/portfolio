package org.example.order.core.infra.common.secrets.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.List;
import java.util.Map;

/**
 * AWS Secrets Manager에서 주기적으로 JSON 키셋을 가져와 파싱 + Resolver에 등록.
 * - @Component 금지(설정 파일에서 조건부로만 등록)
 */
@Slf4j
public class SecretsLoader {

    private final SecretsManagerProperties properties;
    private final SecretsKeyResolver secretsKeyResolver;
    private final SecretsManagerClient secretsManagerClient;
    private final List<SecretKeyRefreshListener> refreshListeners;

    public SecretsLoader(SecretsManagerProperties properties,
                         SecretsKeyResolver secretsKeyResolver,
                         SecretsManagerClient secretsManagerClient,
                         List<SecretKeyRefreshListener> refreshListeners) {
        this.properties = properties;
        this.secretsKeyResolver = secretsKeyResolver;
        this.secretsManagerClient = secretsManagerClient;
        this.refreshListeners = refreshListeners;
    }

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

            Map<String, CryptoKeySpec> parsedKeys = ObjectMapperUtils.readValue(
                    secretJson,
                    new TypeReference<>() {
                    }
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

            refreshListeners.forEach(listener -> {
                try {
                    listener.onSecretKeyRefreshed();
                    log.info("[SecretsLoader] SecretKeyRefreshListener [{}] notified.",
                            listener.getClass().getSimpleName());
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
