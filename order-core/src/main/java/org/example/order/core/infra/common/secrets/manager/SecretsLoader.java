package org.example.order.core.infra.common.secrets.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SecretsLoader {

    private final SecretsManagerProperties properties;
    private final SecretsKeyResolver secretsKeyResolver;
    private final SecretsManagerClient secretsManagerClient;
    private final List<SecretKeyRefreshListener> refreshListeners;

    @Nullable
    private final TaskScheduler taskScheduler;

    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> scheduledFuture;

    public SecretsLoader(SecretsManagerProperties properties,
                         SecretsKeyResolver secretsKeyResolver,
                         SecretsManagerClient secretsManagerClient,
                         List<SecretKeyRefreshListener> refreshListeners,
                         @Nullable TaskScheduler taskScheduler) {
        this.properties = properties;
        this.secretsKeyResolver = secretsKeyResolver;
        this.secretsManagerClient = secretsManagerClient;
        this.refreshListeners = refreshListeners;
        this.taskScheduler = taskScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            refreshOnce();

            log.info("SecretsLoader 초기 로드 완료");
        } catch (Exception e) {
            log.error("SecretsLoader 초기 로드 실패", e);

            if (properties.getSecretsManager().isFailFast() && !isLocalstackEndpoint()) {
                throw new IllegalStateException("SecretsLoader 초기 로드 실패", e);
            } else {
                log.warn("SecretsLoader 초기 로드 실패했지만 LocalStack 환경으로 간주하여 기동을 계속합니다.");
            }
        }

        if (properties.getSecretsManager().isSchedulerEnabled() && taskScheduler != null && scheduled.compareAndSet(false, true)) {
            long delay = Math.max(1_000L, properties.getSecretsManager().getRefreshIntervalMillis());

            scheduledFuture = taskScheduler.scheduleWithFixedDelay(this::safeRefresh, Duration.ofMillis(delay));

            log.info("SecretsLoader 주기 갱신 등록됨. 지연 밀리초={}", delay);
        } else {
            log.info("SecretsLoader 주기 갱신 미등록. scheduler 비활성 또는 스케줄러 미제공");
        }
    }

    public void refreshNowForTest() {
        try {
            refreshOnce();
        } catch (Exception e) {
            throw new IllegalStateException("SecretsLoader 수동 갱신 실패", e);
        }
    }

    private void safeRefresh() {
        try {
            refreshOnce();
        } catch (Exception e) {
            log.error("SecretsLoader 주기 갱신 실패", e);
        }
    }

    /**
     * 실제 1회 로드 로직.
     * - LocalStack(엔드포인트 지정)에서 Secret이 없으면 자동 생성 후 빈 JSON("{}")으로 초기화
     * - 실제 AWS에서는 기존 동작 유지(없으면 예외 → fail-fast 설정에 따름)
     */
    private void refreshOnce() throws Exception {
        final String secretName = properties.getSecretsManager().getSecretName();

        if (secretName == null || secretName.isBlank()) {
            String msg = "aws.secrets-manager.secret-name 이 비었습니다.";

            if (properties.getSecretsManager().isFailFast() && !isLocalstackEndpoint()) {
                throw new IllegalStateException(msg);
            }

            log.warn("{} LocalStack 환경으로 간주하여 건너뜁니다.", msg);

            return;
        }

        String secretJson;

        try {
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(
                    GetSecretValueRequest.builder().secretId(secretName).build()
            );

            secretJson = response.secretString();
        } catch (ResourceNotFoundException rnfe) {

            if (isLocalstackEndpoint()) {
                log.warn("Secret [{}] 이(가) 존재하지 않습니다. LocalStack으로 판단되어 자동 생성합니다.", secretName);

                bootstrapSecretIfMissing(secretName);

                GetSecretValueResponse response = secretsManagerClient.getSecretValue(
                        GetSecretValueRequest.builder().secretId(secretName).build()
                );

                secretJson = response.secretString();
            } else {
                throw rnfe;
            }
        }

        if (secretJson == null || secretJson.isBlank()) {
            log.warn("Secret [{}] 내용이 비어 있습니다. 파싱을 건너뜁니다.", secretName);

            return;
        }

        Map<String, CryptoKeySpec> parsedKeys = parseSecretJson(secretJson);

        parsedKeys.forEach((keyName, spec) -> {
            byte[] decoded = spec.decodeKey();
            int expectedBytes = spec.getKeySize() / 8;

            if (decoded.length != expectedBytes) {
                throw new IllegalArgumentException(
                        String.format("키 길이 오류. 이름=%s 기대바이트=%d 실제바이트=%d",
                                keyName, expectedBytes, decoded.length)
                );
            }

            secretsKeyResolver.updateKey(keyName, spec);
        });

        if (refreshListeners != null) {
            for (SecretKeyRefreshListener listener : refreshListeners) {
                try {
                    listener.onSecretKeyRefreshed();

                    log.info("SecretsLoader 리스너 알림 완료. 리스너={}", listener.getClass().getSimpleName());
                } catch (Exception ex) {
                    log.error("SecretsLoader 리스너 알림 실패. 리스너={}", listener.getClass().getSimpleName(), ex);
                }
            }
        }

        log.info("SecretsLoader 로드 성공. 총 개수={}", parsedKeys.size());
    }

    private Map<String, CryptoKeySpec> parseSecretJson(String secretJson) throws Exception {
        try {
            return ObjectMapperUtils.readValue(secretJson, new TypeReference<Map<String, CryptoKeySpec>>() {
            });
        } catch (Exception e) {
            if (isLocalstackEndpoint() && !properties.getSecretsManager().isFailFast()) {
                log.warn("Secret JSON 파싱 실패(완화). 내용: {}", secretJson, e);

                return Collections.emptyMap();
            }

            throw e;
        }
    }

    /**
     * LocalStack 환경으로 간주되면, 존재하지 않는 Secret을 "{}"로 자동 생성한다.
     */
    private void bootstrapSecretIfMissing(String secretName) {
        final String initial = "{}";

        try {
            // 먼저 create 시도
            secretsManagerClient.createSecret(
                    CreateSecretRequest.builder()
                            .name(secretName)
                            .secretString(initial)
                            .build()
            );

            log.info("Secret [{}] 생성 완료(LocalStack).", secretName);
        } catch (ResourceExistsException exists) {
            secretsManagerClient.putSecretValue(
                    PutSecretValueRequest.builder()
                            .secretId(secretName)
                            .secretString(initial)
                            .build()
            );

            log.info("Secret [{}] 이미 존재. 초기값으로 put 갱신(LocalStack).", secretName);
        } catch (SdkServiceException sse) {
            log.warn("Secret [{}] 생성 중 경고(LocalStack): {}", secretName, sse.getMessage());
        }
    }

    private boolean isLocalstackEndpoint() {
        String ep = properties.getEndpoint();

        if (ep == null || ep.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(ep);
            String host = uri.getHost() != null ? uri.getHost() : "";

            return host.equals("localhost") || host.equals("127.0.0.1");
        } catch (Exception ignore) {
            return false;
        }
    }

    public void cancelSchedule() {
        ScheduledFuture<?> f = this.scheduledFuture;

        if (f != null) {
            f.cancel(false);
        }
    }
}
